(ns closures-and-actors.bank-account
  (:require [clojure.core.async :as async])
  (:require [closures-and-actors.actors :refer :all])
  (:gen-class))

(defn bank-account
  ([]
   (bank-account 0))
  ([current-balance]
   (bank-account current-balance []))
  ([current-balance transaction-history]
   (letfn [(bank-account-behavior [message]
             (case (:type message)
               :credit (bank-account (+ current-balance (:amount message)) (conj transaction-history message))
               :debit (bank-account (- current-balance (:amount message)) (conj transaction-history message))
               :current-balance (do (println "Current balance is:" current-balance)
                                    (bank-account current-balance transaction-history))
               :list-transactions (do (doseq [transaction transaction-history] (println "Transaction:" (:type transaction) (:amount transaction)))
                                      (bank-account current-balance transaction-history))))]
     bank-account-behavior)))


(defn -main [& args]
  (let [bank-account
        (build-actor bank-account)]
    (send-sync bank-account {:type :credit :amount 1000})
    (send-sync bank-account {:type :debit :amount 300})
    (send-sync bank-account {:type :current-balance})
    (send-sync bank-account {:type :list-transactions}))
  (read-line))