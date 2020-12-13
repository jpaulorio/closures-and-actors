(ns closures-and-actors.actors.core-async
  (:require [closures-and-actors.actors.actors-base :refer :all])
  (:require [clojure.core.async :as async])
  (:require [while-let.core :refer :all]))

(defn build-core-async-actor [actor buffer & initial-state]
  (letfn [(core-async-processor [input actor-behavior]
            (async/go
              (let [message (async/<! input)]
                (actor-behavior message)))
            input)]
    (let [channel (async/chan buffer)]
      (apply build-generic-actor actor (partial core-async-processor channel) initial-state))))

(defn send-async [actor message]
  (async/go (async/>! actor message)))

(defn send-sync [actor message]
  (async/>!! actor message))