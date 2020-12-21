(ns closures-and-actors.price-computation.multiple-actors.kafka-test
  (:require [clojure.test :refer :all])
  (:require [clojure.core.async :as async])
  (:require [closures-and-actors.price-computation.multiple-actors.kafka :refer [price-computation-actor
                                                                                 cost-change-actor
                                                                                 new-product-actor]]))

(deftest new-product-actor-test
  (testing "Creating new product message handler actor"
    (testing "should process store channel product"
      (let [actor-stub (async/chan 1)
            send-async #(async/>!! %1 %2)
            test-message {:price-calculation-actor actor-stub :channel :store :product-id 123 :processed false}
            expected-message {:price-calculation-actor actor-stub :channel :store :product-id 123 :processed true}
            actor-behavior (new-product-actor 1 2 send-async)]
        (do (actor-behavior test-message)
            (is (= (async/<!! actor-stub) expected-message)))))
    (testing "should process online channel product"
      (let [actor-stub (async/chan 1)
            send-async #(async/>!! %1 %2)
            test-message {:price-calculation-actor actor-stub :channel :online :product-id 123 :processed false}
            expected-message {:price-calculation-actor actor-stub :channel :online :product-id 123 :processed true}
            actor-behavior (new-product-actor 1 2 send-async)]
        (do (actor-behavior test-message)
            (is (= (async/<!! actor-stub) expected-message)))))))

(deftest cost-change-actor-test
  (testing "Creating cost change message handler actor"
    (testing "should process store channel product"
      (let [actor-stub (async/chan 1)
            send-async #(async/>!! %1 %2)
            test-message {:price-calculation-actor actor-stub :channel :store :product-id 123 :processed false}
            expected-message {:price-calculation-actor actor-stub :channel :store :product-id 123 :processed true}
            actor-behavior (cost-change-actor 1 2 send-async)]
        (do (actor-behavior test-message)
            (is (= (async/<!! actor-stub) expected-message)))))
    (testing "should process online channel product"
      (let [actor-stub (async/chan 1)
            send-async #(async/>!! %1 %2)
            test-message {:price-calculation-actor actor-stub :channel :online :product-id 123 :processed false}
            expected-message {:price-calculation-actor actor-stub :channel :online :product-id 123 :processed true}
            actor-behavior (cost-change-actor 1 2 send-async)]
        (do (actor-behavior test-message)
            (is (= (async/<!! actor-stub) expected-message)))))))

(deftest price-computation-actor-test
  (testing "Creating price computation actor"
    (testing "should process store channel product"
      (let [actor-stub (async/chan 1)
            send-async #(async/>!! %1 %2)
            initial-product {:product-id 123 :price 0.00 :channel :store}
            test-message {:price-calculation-actor actor-stub :channel :store :product-id 123}
            expected-message {:channel :store :product-id 123}
            actor-behavior (price-computation-actor send-async initial-product actor-stub [])]
        (do (actor-behavior test-message)
            (let [result (async/<!! actor-stub)
                  price (:price result)]
              (is (= (dissoc result :price) expected-message))
              (is (not (nil? price)))
              (is (> price 0))))))
    (testing "should process online channel product"
      (let [actor-stub (async/chan 1)
            send-async #(async/>!! %1 %2)
            initial-product {:product-id 123 :price 0.00 :channel :online}
            test-message {:price-calculation-actor actor-stub :channel :online :product-id 123}
            expected-message {:channel :online :product-id 123}
            actor-behavior (price-computation-actor send-async initial-product actor-stub [])]
        (do (actor-behavior test-message)
            (let [result (async/<!! actor-stub)
                  price (:price result)]
              (is (= (dissoc result :price) expected-message))
              (is (not (nil? price)))
              (is (> price 0))))))))
