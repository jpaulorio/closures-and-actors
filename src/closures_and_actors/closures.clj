(ns closures-and-actors.closures
  (:gen-class))

(defn bank-account
  ([] (bank-account 0))
  ([current-balance] (bank-account current-balance []))
  ([current-balance transaction-history]
   {:debit-account     #(bank-account (- current-balance %) (conj transaction-history {:type :debit :amount %}))
    :credit-account    #(bank-account (+ current-balance %) (conj transaction-history {:type :credit :amount %}))
    :current-balance   #(println "Current balance is:" current-balance)
    :list-transactions #(doseq [transaction transaction-history] (println "Transaction:" (:type transaction) (:amount transaction)))}))

(defn -main [& args]
  (let [initial-account-state (bank-account)
        account-after-credit ((:credit-account initial-account-state) 1000)
        account-after-debit ((:debit-account account-after-credit) 300)]
    ((:current-balance account-after-debit))
    ((:list-transactions account-after-debit))))