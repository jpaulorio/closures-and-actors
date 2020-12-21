(ns closures-and-actors.actors.actors-base
  (:require [clojure.core.async :as async])
  (:require [while-let.core :refer :all]))

(defn build-generic-actor [actor message-processor & initial-state]
  (letfn [(behave [behavior]
            (message-processor #(behave (behavior %))))]
    (behave
      (if initial-state
        (actor initial-state)
        (actor)))))