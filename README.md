# Closures and Actors

Just me playing with closures and actors. I'm simulating a price computation engine.

In order to simulate a cpu-bound price computation I'm doing some operations with matrices.

Then I have two main namespaces, one has a [single actor](src/closures_and_actors/price_computation/single_actor/core_async.clj)
for computing prices for all products and another one with
[one actor per product](src/closures_and_actors/price_computation/multiple_actors/core_async.clj).

For the multiple actors version I also have a [Kafka implementation](src/closures_and_actors/price_computation/multiple_actors/kafka.clj).
Both versions share a [common business logic](src/closures_and_actors/price_computation/multiple_actors/domain.clj).

This is an improved version of [this code](https://github.com/jpaulorio/clojure-async-sandbox) since
I was able to completely separate the business logic from the underlying transport medium (core.async/Kafka).
It wouldn't be hard to implement another actor that instead of sending messages through core.async or Kafka, would
send messages through http instead.

The main achievement of this code is that I was able to extract the transport layer used to send messages
across actors, so I can choose between core.async, Kafka, http or something else, without having to duplicate the actor logic .

Both use closures to store state. I'm kinda implementing some sort of actor model here where I have
functions that represent the actor's behavior and another function (build-core-async-actor/build-kafka-actor)
to create an instance of an actor.

There are also two other namespaces in this repository: one demonstrates the
[use of closures to store state](src/closures_and_actors/closures/bank_account.clj),
and the other one is another example of my actor model
implementation with [synchronous messages](src/closures_and_actors/bank_account/domain.clj) using
[core.async](src/closures_and_actors/bank_account/core_async.clj) and
[Kafka](src/closures_and_actors/bank_account/kafka.clj).

## Usage

To build the uberjar only:

    $ ./build.sh

To build and run with default args:

    $ ./build-and-run.sh

Run the following command to run both single-actor and multiple-actors modes
**with core.async transport**  computing 10,000 prices for 100 products:

    $ ./run.sh

## Options

    $ ./run.sh [mode] [number-of-products] [number-of-events]

## Examples

    $ ./run.sh sa 100 1000

    $ ./run.sh ma 1000 10000

Run the following command to run the multiple-actors mode
**with Kafka transport** computing 10,000 prices for 100 products **(Docker required)**:

    $ ./run-kafka.sh
    
Run the following command to run the bank account example with core.async transport:

    $ ./run-bank-account.sh

Run the following command to run the bank account example with Kafka transport **(Docker required)**:

    $ ./run-bank-account-kafka.sh

Run the following command to run the closures example:

    $ ./run-closures.sh

Where:

mode: sa or ma

number-of-products: any positive integer

number-of-events: any positive integer

### Bugs

Probably a lot.

### Disclaimer

This is a just a toy project, so I can learn about closures, actors and clojure.core.async and should not be seen as a production-grade solution.

## License

Copyright © 2020 JP Silva

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
