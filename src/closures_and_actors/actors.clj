(ns closures-and-actors.actors
  (:require [clojure.core.async :as async])
  (:require [while-let.core :refer :all]))

(defn build-generic-actor [actor message-processor & initial-state]
  (letfn [(behave [behavior]
            (message-processor #(behave (behavior %))))]
    (behave
      (if initial-state
        (apply actor initial-state)
        (actor)))))