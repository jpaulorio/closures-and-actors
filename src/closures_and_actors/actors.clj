(ns closures-and-actors.actors
  (:require [clojure.core.async :as async])
  (:require [while-let.core :refer :all]))

(defn build-actor [actor & initial-state]
  (letfn [(behave [behavior]
            (let [current-input (async/chan)]
              (async/go
                (let [message (async/<! current-input)]
                  (let [new-input (behave (behavior message))]
                    (while-let [m (async/<! current-input)]
                               (async/>! new-input m))
                    (async/close! current-input))))
              current-input))]
    (behave
      (if initial-state
        (apply actor initial-state)
        (actor)))))

(defn send-async [actor message]
  (async/go (async/>! actor message)))

(defn send-sync [actor message]
  (async/>!! actor message))