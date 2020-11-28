(ns closures-and-actors.single-actor
  (:require [closures-and-actors.common :refer :all])
  (:require [closures-and-actors.actors :refer :all])
  (:require [clojure.core.async :as async])
  (:require [while-let.core :refer :all])
  (:gen-class))

(defn new-product-handler [store-products-count online-products-count price-calculation-actor]
  (letfn [(product-handler-behavior [message]
            (case (:channel message)
              :store (let [product-id (:product-id message)]
                       (println "Processing new store product with id:" product-id)
                       (send-async price-calculation-actor message)
                       (new-product-handler (inc store-products-count) online-products-count price-calculation-actor))
              :online (let [product-id (:product-id message)]
                        (println "Processing new online product with id:" product-id)
                        (send-async price-calculation-actor message)
                        (new-product-handler store-products-count (inc online-products-count) price-calculation-actor))))]
    product-handler-behavior))

(defn cost-change-handler [store-products-count online-products-count price-calculation-actor]
  (letfn [(cost-handler-behavior [message]
            (case (:channel message)
              :store (let [product-id (:product-id message)]
                       (println "Processing cost change for store product with id:" product-id)
                       (send-async price-calculation-actor message)
                       (cost-change-handler (inc store-products-count) online-products-count price-calculation-actor))
              :online (let [product-id (:product-id message)]
                        (println "Processing cost change for online product with id:" (:product-id message))
                        (send-async price-calculation-actor message)
                        (cost-change-handler store-products-count (inc online-products-count) price-calculation-actor))))]
    cost-handler-behavior))

(defn price-computation-handler [product-list output-channel]
  (letfn [(price-computation-handler-behavior [message]
            (case (:channel message)
              :store (let [product-id (:product-id message)
                           updated-product (assoc message :price (compute-price))]
                       (println "Computing price for store product with id:" product-id)
                       (send-async output-channel updated-product)
                       (price-computation-handler (assoc product-list product-id updated-product) output-channel))
              :online (let [product-id (:product-id message)
                            updated-product (assoc message :price (compute-price))]
                        (println "Computing price for online product with id:" product-id)
                        (send-async output-channel updated-product)
                        (price-computation-handler (assoc product-list product-id updated-product) output-channel))))]
    price-computation-handler-behavior))

(defn run-simulation [number-of-products number-of-events]
  (let [products (vec (generate-products-without-channels number-of-products))
        new-price-output (async/chan)
        price-computation-handler-actor (build-core-async-actor price-computation-handler products new-price-output)
        new-product-handler-actor (build-core-async-actor new-product-handler 0 0 price-computation-handler-actor)
        cost-change-handler-actor (build-core-async-actor cost-change-handler 0 0 price-computation-handler-actor)
        event-types [:new-product :cost-change]
        event-actor-map {:new-product new-product-handler-actor :cost-change cost-change-handler-actor}
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
