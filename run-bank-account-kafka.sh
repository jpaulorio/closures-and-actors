docker-compose up -d &&
java -cp target/uberjar/closures-and-actors-0.1.0-SNAPSHOT-standalone.jar closures_and_actors.bank_account.kafka &&
docker-compose down