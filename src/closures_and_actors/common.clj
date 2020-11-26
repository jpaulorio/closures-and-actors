(ns closures-and-actors.common
  (:require [clojure.core.matrix :as m])
  (:require [clojure.core.async :as async])
  (:gen-class))

(defn round-places [number decimals]
  (let [factor (Math/pow 10 decimals)]
    (double (/ (Math/round (* factor number)) factor))))

(defn compute-price []
  (let [matrix-size 2000
        A [(vec (repeatedly matrix-size #(rand 5))) (vec (repeatedly matrix-size #(rand 5)))]
        B [(vec (repeatedly matrix-size #(rand 5))) (vec (repeatedly matrix-size #(rand 5)))]]
    (round-places (/ (apply + (map (partial reduce +) (m/mul A B))) (rand 100000)) 2)))

(defn find-product [message product-list]
  (first (filter #(= (:product-id %) (:product-id message)) product-list)))

(defn pick-random-sales-channel []
  (let [sales-channel-types [:store :online]]
    (nth sales-channel-types (rand-int (count sales-channel-types)))))

(defn generate-products-without-channels [product-count]
  (map #(-> {:product-id % :price 0.00 :channel (pick-random-sales-channel)}) (range product-count)))

(defn generate-products-with-channels [product-count]
  (map #(-> {:product-id % :price 0.00 :channel (pick-random-sales-channel) :input-channel (async/chan 2) :output-channel (async/chan 2)}) (range product-count)))

(defn pick-random-event-channel [event-types event-handler-map]
  (let [event-type (nth event-types (rand-int (count event-types)))]
    (event-type event-handler-map)))

(defn pick-random-product [products]
  (nth products (rand-int (count products))))

(defn close-channels [channels]
  (doseq [channel channels]
    (async/close! channel)))