(ns closures-and-actors.actors.kafka
  (:require [closures-and-actors.actors.actors-base :refer :all])
  (:require [while-let.core :refer :all])
  (:require [clojure.core.async :as async])
  (:import [org.apache.kafka.clients.admin AdminClient AdminClientConfig NewTopic]
           org.apache.kafka.clients.consumer.KafkaConsumer
           [org.apache.kafka.clients.producer KafkaProducer ProducerRecord]
           [org.apache.kafka.common.serialization StringDeserializer StringSerializer]
           (org.apache.kafka.common TopicPartition)
           (java.time Duration)))

(defn build-producer ^KafkaProducer
  ;"Create the kafka producer to send on messages received"
  [bootstrap-server]
  (let [producer-props {"value.serializer"  StringSerializer
                        "key.serializer"    StringSerializer
                        "bootstrap.servers" bootstrap-server}]
    (KafkaProducer. producer-props)))

(defn build-consumer
  "Create the consumer instance to consume
from the provided kafka topic name"
  [bootstrap-server]
  (let [consumer-props
        {"bootstrap.servers"  bootstrap-server
         "group.id"           "example"
         "key.deserializer"   StringDeserializer
         "value.deserializer" StringDeserializer
         "auto.offset.reset"  "earliest"
         "enable.auto.commit" "true"
         "max.poll.records"   (int 1)}]
    (KafkaConsumer. consumer-props)))

(defn consumer-subscribe
  [consumer topic]
  (.subscribe consumer [topic]))

(defn send-async [actor message]
  (let [bootstrap-server "localhost:19092"
        producer (build-producer bootstrap-server)]
    (.send producer (ProducerRecord. actor (str message)))))

(defn send-sync [actor message]
  (let [bootstrap-server "localhost:19092"
        producer (build-producer bootstrap-server)]
    (.get (.send producer (ProducerRecord. actor (str message))))))

(defn read-sync [consumer]
  (if-let [message (first (.poll consumer (Duration/ofMillis 100)))]
    (read-string (.value message))))

(defn build-kafka-actor [actor topic & initial-state]
  (letfn [(kafka-processor [consumer topic actor-behavior]
            (if-let [message (first (.poll consumer (Duration/ofMillis 100)))]
              (async/thread (actor-behavior (read-string (.value message))))
              (async/thread (actor-behavior nil)))
            topic)]
    (let [bootstrap-server "localhost:19092"
          consumer (build-consumer bootstrap-server)]
      (consumer-subscribe consumer topic)
      (apply build-generic-actor
             (fn
               ([]
                (actor send-async))
               ([state]
                (if state
                  (apply actor send-async state)
                  (actor send-async))))
             (partial kafka-processor consumer topic)
             initial-state))))

(defn create-topics!
  "Create the topics "
  [bootstrap-server topics ^Integer partitions ^Short replication]
  (let [config {AdminClientConfig/BOOTSTRAP_SERVERS_CONFIG bootstrap-server}
        adminClient (AdminClient/create config)
        new-topics (map (fn [^String topic-name] (NewTopic. topic-name partitions replication)) topics)]
    (.createTopics adminClient new-topics)))