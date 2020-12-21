(ns closures-and-actors.price-computation.multiple-actors.domain
  (:require [closures-and-actors.price-computation.common :refer :all])
  (:gen-class))

(defn new-product-actor [send-async store-products-count online-products-count]
  (letfn [(product-actor-behavior [message]
            (case (:channel message)
              :store (let [price-calculation-actor (:price-calculation-actor message)]
                       (println "Processing new store product with id:" (:product-id message))
                       (send-async price-calculation-actor (assoc message :processed true))
                       (new-product-actor send-async (inc store-products-count) online-products-count))
              :online (let [price-calculation-actor (:price-calculation-actor message)]
                        (println "Processing new online product with id:" (:product-id message))
                        (send-async price-calculation-actor (assoc message :processed true))
                        (new-product-actor send-async store-products-count (inc online-products-count)))
              (new-product-actor send-async store-products-count online-products-count)))]
    product-actor-behavior))

(defn cost-change-actor [send-async store-products-count online-products-count]
  (letfn [(cost-actor-behavior [message]
            (case (:channel message)
              :store (let [price-calculation-actor (:price-calculation-actor message)]
                       (println "Processing cost change for store product with id:" (:product-id message))
                       (send-async price-calculation-actor (assoc message :processed true))
                       (cost-change-actor send-async (inc store-products-count) online-products-count))
              :online (let [price-calculation-actor (:price-calculation-actor message)]
                        (println "Processing cost change for online product with id:" (:product-id message))
                        (send-async price-calculation-actor (assoc message :processed true))
                        (cost-change-actor send-async store-products-count (inc online-products-count)))
              (cost-change-actor send-async store-products-count online-products-count)))]
    cost-actor-behavior))

(defn price-computation-actor [send-async current-product output-channel price-history]
  (letfn [(price-computation-actor-behavior [message]
            (case (:channel message)
              :store (let [current-price (:price current-product)
                           updated-product (assoc current-product :price (round-places (+ current-price (compute-price)) 2))]
                       (println "Computing price for store product:" (dissoc current-product :price-calculation-actor))
                       (send-async output-channel updated-product)
                       (price-computation-actor send-async updated-product output-channel (cons (:price updated-product) price-history)))
              :online (let [current-price (:price current-product)
                            updated-product (assoc current-product :price (round-places (+ current-price (compute-price)) 2))]
                        (println "Computing price for online product:" (dissoc current-product :price-calculation-actor))
                        (send-async output-channel updated-product)
                        (price-computation-actor send-async updated-product output-channel (cons (:price updated-product) price-history)))
              :list-history (let [product-id (:product-id current-product)]
                              (println (str "Price history for product " product-id ": " price-history))
                              (price-computation-actor send-async current-product output-channel price-history))
              (price-computation-actor send-async current-product output-channel price-history)))]
    price-computation-actor-behavior))