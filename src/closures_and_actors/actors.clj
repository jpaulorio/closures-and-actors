(ns closures-and-actors.actors
  (:require [clojure.core.async :as async])
  (:require [while-let.core :refer :all]))

(defn build-generic-actor [actor process-messages & initial-state]
  (letfn [(behave [behavior]
            (process-messages #(behave (behavior %))))]
    (behave
      (if initial-state
        (apply actor initial-state)
        (actor)))))

(defn build-core-async-actor [actor & initial-state]
  (letfn [(core-async-processor [generate-new-state]
            (let [current-input (async/chan)]
              (async/go
                (let [message (async/<! current-input)
                      new-input (generate-new-state message)]
                  (while-let [m (async/<! current-input)]
                             (async/>! new-input m))
                  (async/close! current-input)))
              current-input))]
    (apply build-generic-actor actor core-async-processor initial-state)))

(defn send-async [actor message]
  (async/go (async/>! actor message)))

(defn send-sync [actor message]
  (async/>!! actor message))