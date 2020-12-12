(ns closures-and-actors.kafka-actors
  (:require [closures-and-actors.actors :refer :all])
  (:require [while-let.core :refer :all])
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
         "auto.offset.reset"  "latest"
         "enable.auto.commit" "true"
         "max.poll.records"   (int 1)}]
    (KafkaConsumer. consumer-props)))

(defn consumer-subscribe
  [consumer topic]
  (.subscribe consumer [topic]))

(defn build-kafka-actor [actor topic & initial-state]
  (letfn [(kafka-processor [consumer topic actor-behavior]
            (let [message (first (.poll consumer (Duration/ofMillis 100)))]
              (future (actor-behavior (if message (read-string (.value message))))))
            topic)]
    (let [bootstrap-server "localhost:19092"
          consumer (build-consumer bootstrap-server)]
      (consumer-subscribe consumer topic)
      (apply build-generic-actor actor (partial kafka-processor consumer topic) initial-state))))

(defn send-async [actor message]
  (let [bootstrap-server "localhost:19092"
        producer (build-producer bootstrap-server)]
    (.send producer (ProducerRecord. actor message))))

(defn send-sync [actor message]
  (let [bootstrap-server "localhost:19092"
        producer (build-producer bootstrap-server)]
    (.send producer (ProducerRecord. actor (str message)))))