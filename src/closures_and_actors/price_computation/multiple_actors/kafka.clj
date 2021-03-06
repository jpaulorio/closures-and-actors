(ns closures-and-actors.price-computation.multiple-actors.kafka
  (:require [closures-and-actors.price-computation.common :refer :all])
  (:require [closures-and-actors.actors.actors-base :refer :all])
  (:require [closures-and-actors.actors.kafka :as ak])
  (:require [while-let.core :refer :all])
  (:require [clojure.core.async :as async])
  (:require [closures-and-actors.price-computation.multiple-actors.domain :refer :all])
  (:gen-class))

(defn initialize-kafka [bootstrap-server new-price-output new-product-actor-instance
                        cost-change-actor-instance products-actors-map consumer]
  (ak/create-topics! bootstrap-server [new-price-output new-product-actor-instance cost-change-actor-instance] 1 1)
  (ak/create-topics! bootstrap-server (map :price-calculation-actor products-actors-map) 1 1)
  (ak/consumer-subscribe consumer new-price-output))

(defn send-events-to-actors [number-of-events products-actors-map event-types event-actor-map]
  (doseq [n (range number-of-events)]
    (let [product (pick-random-product products-actors-map)
          event-actor (pick-random-event-channel event-types event-actor-map)]
      (ak/send-async event-actor product))))

(defn consolidate-prices [event-count number-of-events consumer]
  (while (not= @event-count number-of-events)
    (if-let [message (ak/read-sync consumer)]
      (do (swap! event-count inc)
          (println (str (dissoc message :price-calculation-actor) " - " @event-count " of " number-of-events))))))

(defn print-prices-history [products-actors-map]
  (doseq [product products-actors-map]
    (ak/send-sync (:price-calculation-actor product) {:channel :list-history})))

(defn run-simulation [number-of-products number-of-events]
  (println
    "*****************************************************************************
*******************START OF MULTIPLE ACTOR SIM WITH KAFKA********************
*****************************************************************************")
  (let [bootstrap-server "localhost:19092"
        products (vec (generate-products-without-channels number-of-products))
        new-price-output "new-price-output"
        new-product-actor-instance (ak/build-kafka-actor new-product-actor "new-product-actor" 0 0)
        cost-change-actor-instance (ak/build-kafka-actor cost-change-actor "cost-change-actor" 0 0)
        event-types [:new-product :cost-change]
        event-actor-map {:new-product new-product-actor-instance :cost-change cost-change-actor-instance}
        products-actors-map (map #(assoc % :price-calculation-actor
                                           (ak/build-kafka-actor
                                             price-computation-actor
                                             (str "price-calculation-actor-" (:product-id %))
                                             %
                                             new-price-output
                                             []))
                                 products)
        event-count (atom 0)
        consumer (ak/build-consumer bootstrap-server)]

    (println (str "Multiple Actors - Processing " number-of-events " events for " number-of-products " products ..."))

    (initialize-kafka bootstrap-server new-price-output new-product-actor-instance
                      cost-change-actor-instance products-actors-map consumer)

    (send-events-to-actors number-of-events products-actors-map event-types event-actor-map)

    (consolidate-prices event-count number-of-events consumer)

    (print-prices-history products-actors-map)

    (Thread/sleep 1000)))

(defn -main [& args]
  (run-simulation 10 100))
