(ns closures-and-actors.multiple-actors
  (:require [closures-and-actors.common :refer :all])
  (:require [closures-and-actors.actors :refer :all])
  (:require [clojure.core.async :as async])
  (:require [while-let.core :refer :all])
  (:gen-class))

(defn new-product-actor [store-products-count online-products-count]
  (letfn [(product-actor-behavior [message]
            (case (:channel message)
              :store (let [price-calculation-actor (:price-calculation-actor message)]
                       (println "Processing new store product with id:" (:product-id message))
                       (send-async price-calculation-actor message)
                       (new-product-actor (inc store-products-count) online-products-count))
              :online (let [price-calculation-actor (:price-calculation-actor message)]
                        (println "Processing new online product with id:" (:product-id message))
                        (send-async price-calculation-actor message)
                        (new-product-actor store-products-count (inc online-products-count)))))]
    product-actor-behavior))

(defn cost-change-actor [store-products-count online-products-count]
  (letfn [(cost-actor-behavior [message]
            (case (:channel message)
              :store (let [price-calculation-actor (:price-calculation-actor message)]
                       (println "Processing cost change for store product with id:" (:product-id message))
                       (send-async price-calculation-actor message)
                       (cost-change-actor (inc store-products-count) online-products-count))
              :online (let [price-calculation-actor (:price-calculation-actor message)]
                        (println "Processing cost change for online product with id:" (:product-id message))
                        (send-async price-calculation-actor message)
                        (cost-change-actor store-products-count (inc online-products-count)))))]
    cost-actor-behavior))

(defn price-computation-actor [current-product output-channel price-history]
  (letfn [(price-computation-actor-behavior [message]
            (case (:channel message)
              :store (let [updated-product (assoc current-product :price (compute-price))]
                       (println "Computing price for store product:" (dissoc current-product :price-calculation-actor))
                       (send-async output-channel updated-product)
                       (price-computation-actor updated-product output-channel (cons (:price updated-product) price-history)))
              :online (let [updated-product (assoc current-product :price (compute-price))]
                        (println "Computing price for online product:" (dissoc current-product :price-calculation-actor))
                        (send-async output-channel updated-product)
                        (price-computation-actor updated-product output-channel (cons (:price updated-product) price-history)))
              :list-history (let [product-id (:product-id current-product)]
                              (println (str "Price history for product " product-id ": " price-history))
                              (price-computation-actor current-product output-channel price-history))))]
    price-computation-actor-behavior))

(defn run-simulation [number-of-products number-of-events]
  (println
    "*****************************************************************************
************************START OF MULTIPLE ACTOR SIM**************************
*****************************************************************************")
  (let [products (vec (generate-products-without-channels number-of-products))
        new-price-output (async/chan)
        new-product-actor-instance (build-core-async-actor new-product-actor 4000 0 0)
        cost-change-actor-instance (build-core-async-actor cost-change-actor 4000 0 0)
        event-types [:new-product :cost-change]
        event-actor-map {:new-product new-product-actor-instance :cost-change cost-change-actor-instance}
        products-actors-map (map #(assoc % :price-calculation-actor (build-core-async-actor price-computation-actor 1 % new-price-output [])) products)
        event-count (atom 0)]
    ;randomly sends events/messages to actors
    (doseq [n (range number-of-events)]
      (let [product (pick-random-product products-actors-map)
            event-actor (pick-random-event-channel event-types event-actor-map)]
        (send-async event-actor product)))

    (println (str "Multiple Actors - Processing " number-of-events " events for " number-of-products " products ..."))

    ;consolidate computed prices from the new price channel
    (async/go (while-let [message (async/<! new-price-output)]
                         (swap! event-count inc)
                         (println (str (dissoc message :price-calculation-actor) " - " @event-count " of " number-of-events))))

    ;waits until all events are processed
    (while (not= @event-count number-of-events))

    (async/close! new-price-output)

    (doseq [product products-actors-map]
      (send-sync (:price-calculation-actor product) {:channel :list-history}))

    (do
      (println "Hit enter to exit.")
      (read-line))))
