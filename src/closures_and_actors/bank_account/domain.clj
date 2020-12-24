(ns closures-and-actors.bank-account.domain
  (:gen-class))

(defn bank-account
  ([]
   (bank-account nil 0))
  ([_]
   (bank-account _ 0))
  ([_ current-balance]
   (bank-account _ current-balance []))
  ([_ current-balance transaction-history]
   (letfn [(bank-account-behavior [message]
             (case (:type message)
               :credit (bank-account _ (+ current-balance (:amount message)) (conj transaction-history message))
               :debit (bank-account _ (- current-balance (:amount message)) (conj transaction-history message))
               :current-balance (do (println "Current balance is:" current-balance)
                                    (bank-account _ current-balance transaction-history))
               :list-transactions (do (doseq [transaction transaction-history] (println "Transaction:" (:type transaction) (:amount transaction)))
                                      (bank-account _ current-balance transaction-history))
               (bank-account _ current-balance transaction-history)))]
     bank-account-behavior)))