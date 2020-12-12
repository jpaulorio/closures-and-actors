(ns closures-and-actors.multiple-actors-kafka
  (:require [closures-and-actors.common :refer :all])
  (:require [closures-and-actors.actors :refer :all])
  (:require [closures-and-actors.kafka-actors :refer :all])
  (:require [while-let.core :refer :all])
  (:require [clojure.core.async :as async])
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
                        (new-product-actor store-products-count (inc online-products-count)))
              (new-product-actor store-products-count online-products-count)))]
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
                        (cost-change-actor store-products-count (inc online-products-count)))
              (cost-change-actor store-products-count online-products-count)))]
    cost-actor-behavior))

(defn price-computation-actor [current-product output-channel price-history]
  (letfn [(price-computation-actor-behavior [message]
            (case (:channel message)
              :store (let [current-price (:price current-product)
                           updated-product (assoc current-product :price (round-places (+ current-price (compute-price)) 2))]
                       (println "Computing price for store product:" (dissoc current-product :price-calculation-actor))
                       (send-async output-channel updated-product)
                       (price-computation-actor updated-product output-channel (cons (:price updated-product) price-history)))
              :online (let [current-price (:price current-product)
                            updated-product (assoc current-product :price (round-places (+ current-price (compute-price)) 2))]
                        (println "Computing price for online product:" (dissoc current-product :price-calculation-actor))
                        (send-async output-channel updated-product)
                        (price-computation-actor updated-product output-channel (cons (:price updated-product) price-history)))
              :list-history (let [product-id (:product-id current-product)]
                              (println (str "Price history for product " product-id ": " price-history))
                              (price-computation-actor current-product output-channel price-history))
              (price-computation-actor current-product output-channel price-history)))]
    price-computation-actor-behavior))

(defn run-simulation [number-of-products number-of-events]
  (println
    "*****************************************************************************
************************START OF MULTIPLE ACTOR SIM**************************
*****************************************************************************")
  (let [bootstrap-server "localhost:19092"
        products (vec (generate-products-without-channels number-of-products))
        new-price-output "new-price-output"
        new-product-actor-instance (build-kafka-actor new-product-actor "new-product-actor" 0 0)
        cost-change-actor-instance (build-kafka-actor cost-change-actor "cost-change-actor" 0 0)
        event-types [:new-product :cost-change]
        event-actor-map {:new-product new-product-actor-instance :cost-change cost-change-actor-instance}
        products-actors-map (map #(assoc % :price-calculation-actor
                                           (build-kafka-actor
                                             price-computation-actor
                                             (str "price-calculation-actor-" (:product-id %))
                                             %
                                             new-price-output
                                             []))
                                 products)
        event-count (atom 0)
        consumer (build-consumer bootstrap-server)]

    (create-topics! bootstrap-server [new-price-output new-product-actor-instance cost-change-actor-instance] 1 1)
    (create-topics! bootstrap-server (map :price-calculation-actor products-actors-map) 1 1)
    (consumer-subscribe consumer new-price-output)

    ;randomly sends events/messages to actors
    (doseq [n (range number-of-events)]
      (let [product (pick-random-product products-actors-map)
            event-actor (pick-random-event-channel event-types event-actor-map)]
        (send-async event-actor product)))

    (println (str "Multiple Actors - Processing " number-of-events " events for " number-of-products " products ..."))

    ;consolidate computed prices from the new price channel
    ;waits until all events are processed
    (while (not= @event-count number-of-events)
         (if-let [message (read-sync consumer)]
           (do (swap! event-count inc)
               (println (str (dissoc message :price-calculation-actor) " - " @event-count " of " number-of-events)))))

    (doseq [product products-actors-map]
      (send-sync (:price-calculation-actor product) {:channel :list-history}))

    (Thread/sleep 1000)))

(defn -main [& args]
  (run-simulation 2 20))
