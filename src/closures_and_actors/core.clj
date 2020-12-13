(ns closures-and-actors.core
  (:require [closures-and-actors.price-computation.single-actor.core-async :as sa])
  (:require [closures-and-actors.price-computation.multiple-actors.core-async :as ma])
  (:gen-class))


(defn -main [& args]
  (let [mode (if-let [arguments args] (keyword (first arguments)) :both)
        number-of-products (if-let [arguments args] (read-string (second arguments)) 100)
        number-of-events (if-let [arguments args] (read-string (nth arguments 2)) 10000)]
    (case mode
      :sa (sa/run-simulation number-of-products number-of-events)
      :ma (ma/run-simulation number-of-products number-of-events)
      :both (do
              (sa/run-simulation number-of-products number-of-events)
              (ma/run-simulation number-of-products number-of-events)))))