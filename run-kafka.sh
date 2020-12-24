docker-compose up -d &&
java -cp target/uberjar/closures-and-actors-0.1.0-SNAPSHOT-standalone.jar closures_and_actors.price_computation.multiple_actors.kafka &&
docker-compose down