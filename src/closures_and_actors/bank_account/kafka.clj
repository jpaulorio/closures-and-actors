(ns closures-and-actors.bank-account.kafka
  (:require [clojure.core.async :as async])
  (:require [closures-and-actors.bank-account.domain :refer :all])
  (:require [closures-and-actors.actors.actors-base :refer :all])
  (:require [closures-and-actors.actors.kafka :refer :all])
  (:gen-class))

(defn -main [& args]
  (let [bank-account
        (build-kafka-actor bank-account "bank-account-actor")]
    (send-sync bank-account {:type :credit :amount 1000})
    (send-sync bank-account {:type :debit :amount 300})
    (send-sync bank-account {:type :current-balance})
    (send-sync bank-account {:type :list-transactions}))
  (Thread/sleep 1000))