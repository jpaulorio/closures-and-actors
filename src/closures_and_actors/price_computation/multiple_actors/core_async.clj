(ns closures-and-actors.price-computation.multiple-actors.core-async
  (:require [closures-and-actors.price-computation.common :refer :all])
  (:require [closures-and-actors.actors.actors-base :refer :all])
  (:require [closures-and-actors.actors.core-async :refer :all])
  (:require [closures-and-actors.price-computation.multiple-actors.domain :refer :all])
  (:require [clojure.core.async :as async])
  (:require [while-let.core :refer :all])
  (:gen-class))

(defn send-events-to-actors [number-of-events event-types products-actors-map event-actor-map]
  (doseq [n (range number-of-events)]
    (let [product (pick-random-product products-actors-map)
          event-actor (pick-random-event-channel event-types event-actor-map)]
      (send-async event-actor product))))

(defn consolidate-prices [new-price-output event-count number-of-events]
  (async/go (while-let [message (async/<! new-price-output)]
                       (swap! event-count inc)
                       (println (str (dissoc message :price-calculation-actor) " - " @event-count " of " number-of-events)))))

(defn print-prices-history [products-actors-map]
                      (doseq [product products-actors-map]
                        (send-sync (:price-calculation-actor product) {:channel :list-history})))

(defn run-simulation [number-of-products number-of-events]
  (println
    "*****************************************************************************
****************START OF MULTIPLE ACTOR SIM WITH CORE.ASYNC******************
*****************************************************************************")
  (let [products (vec (generate-products-without-channels number-of-products))
        new-price-output (async/chan)
        new-product-actor-instance (build-core-async-actor new-product-actor 5000 0 0)
        cost-change-actor-instance (build-core-async-actor cost-change-actor 5000 0 0)
        event-types [:new-product :cost-change]
        event-actor-map {:new-product new-product-actor-instance :cost-change cost-change-actor-instance}
        products-actors-map (map #(assoc % :price-calculation-actor
                                           (build-core-async-actor
                                             price-computation-actor
                                             1
                                             %
                                             new-price-output
                                             []))
                                 products)
        event-count (atom 0)]

    (println (str "Multiple Actors - Processing " number-of-events " events for " number-of-products " products ..."))

    (send-events-to-actors number-of-events event-types products-actors-map event-actor-map)

    (consolidate-prices new-price-output event-count number-of-events)

    (while (not= @event-count number-of-events))

    (async/close! new-price-output)

    (print-prices-history products-actors-map)

    (Thread/sleep 1000)))

(defn -main [& args]
  (run-simulation 10 100))