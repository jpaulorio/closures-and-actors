(ns closures-and-actors.single-actor
  (:require [closures-and-actors.common :refer :all])
  (:require [closures-and-actors.actors :refer :all])
  (:require [clojure.core.async :as async])
  (:require [while-let.core :refer :all])
  (:gen-class))

(defn new-product-actor [store-products-count online-products-count price-calculation-actor]
  (letfn [(product-actor-behavior [message]
            (case (:channel message)
              :store (let [product-id (:product-id message)]
                       (println "Processing new store product with id:" product-id)
                       (send-async price-calculation-actor message)
                       (new-product-actor (inc store-products-count) online-products-count price-calculation-actor))
              :online (let [product-id (:product-id message)]
                        (println "Processing new online product with id:" product-id)
                        (send-async price-calculation-actor message)
                        (new-product-actor store-products-count (inc online-products-count) price-calculation-actor))))]
    product-actor-behavior))

(defn cost-change-actor [store-products-count online-products-count price-calculation-actor]
  (letfn [(cost-actor-behavior [message]
            (case (:channel message)
              :store (let [product-id (:product-id message)]
                       (println "Processing cost change for store product with id:" product-id)
                       (send-async price-calculation-actor message)
                       (cost-change-actor (inc store-products-count) online-products-count price-calculation-actor))
              :online (let [product-id (:product-id message)]
                        (println "Processing cost change for online product with id:" (:product-id message))
                        (send-async price-calculation-actor message)
                        (cost-change-actor store-products-count (inc online-products-count) price-calculation-actor))))]
    cost-actor-behavior))

(defn price-computation-actor [product-list output-channel]
  (letfn [(price-computation-actor-behavior [message]
            (case (:channel message)
              :store (let [product-id (:product-id message)
                           updated-product (assoc message :price (compute-price))]
                       (println "Computing price for store product with id:" product-id)
                       (send-async output-channel updated-product)
                       (price-computation-actor (assoc product-list product-id updated-product) output-channel))
              :online (let [product-id (:product-id message)
                            updated-product (assoc message :price (compute-price))]
                        (println "Computing price for online product with id:" product-id)
                        (send-async output-channel updated-product)
                        (price-computation-actor (assoc product-list product-id updated-product) output-channel))))]
    price-computation-actor-behavior))

(defn run-simulation [number-of-products number-of-events]
  (println "*****************************************************************************
**************************START OF SINGLE ACTOR SIM**************************
*****************************************************************************")
  (let [products (vec (generate-products-without-channels number-of-products))
        new-price-output (async/chan)
        price-computation-actor (build-core-async-actor price-computation-actor products new-price-output)
        new-product-actor-instance (build-core-async-actor new-product-actor 0 0 price-computation-actor)
        cost-change-actor-instance (build-core-async-actor cost-change-actor 0 0 price-computation-actor)
        event-types [:new-product :cost-change]
        event-actor-map {:new-product new-product-actor-instance :cost-change cost-change-actor-instance}
        event-count (atom 0)]
    ;randomly sends events/messages to actors
    (doseq [n (range number-of-events)]
      (send-async (pick-random-event-channel event-types event-actor-map) (pick-random-product products)))
    (println (str "Single Actor - Processing " number-of-events " events for " number-of-products " products ..."))

    ;consolidate computed prices from the new price channel
    (async/go (while-let [message (async/<! new-price-output)]
                         (swap! event-count inc)
                         (println (str message " - " @event-count " of " number-of-events))))

    ;waits until all events are processed
    (while (not= @event-count number-of-events))
    (async/close! new-price-output)))
