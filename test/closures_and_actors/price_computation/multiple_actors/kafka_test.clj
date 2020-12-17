(ns closures-and-actors.price-computation.multiple-actors.kafka-test
  (:require [clojure.test :refer :all])
  (:require [closures-and-actors.price-computation.multiple-actors.kafka :refer [price-computation-actor
                                                                                 cost-change-actor
                                                                                 new-product-actor]]))

(deftest price-computation-actor-test
  )

(deftest cost-change-actor-test
  )

(deftest new-product-actor-test
  (testing "Creating new product message handler actor"
    (testing "constructor"
      (let [actor-behavior (new-product-actor 1 2)
            ]
        ))))
