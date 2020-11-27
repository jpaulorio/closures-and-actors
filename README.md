# closures-and-actors

Just me playing with closures, actors and core.async. I'm simulating a price computation engine.

In order to simulate a cpu-bound price computation I'm doing some operations with matrices.

Then I have two main namespaces, one has a single actor for computing prices for all products and another one with one actor per product.

This is an improved version of [this code](https://github.com/jpaulorio/clojure-async-sandbox) since I was able to completely separate the business logic from the underlying transport medium (core.async).
It wouldn't be hard to implement an actor that instead of sending messages through core.async, would send messages through http.

One thing I want to try later is to extract the transport layer used to send messages across actors, so I can choose between core.async, http or something else, without having to duplicate the actor logic .

Both use closures to store state. I'm kinda implementing some sort of actor model here where I have functions that represent the actor's behavior and another function (closures-and-actors.actors/build-actor) to create an instance of an actor.

There are also two other namespaces: one demonstrates the use of closures to store state (closures-and-actors.closures), and the other one is another example of my actor model implementation with synchronous messages (closures-and-actors.bank-account).

## Usage

To build the uberjar only:

    $ ./build.sh

To build and run with default args:

    $ ./build-and-run.sh

Run the following command to run both programs computing prices for 50 products:

    $ ./run.sh
    
Run the following command to run the bank account example:

    $ ./run-bank-account.sh

Run the following command to run the closures example:

    $ ./run-closures.sh

## Options

    $ ./run.sh [mode] [number-of-products]

Where:

mode - sh or mh

number-of-products - any positive integer

## Examples

    $ ./run.sh sh 100

    $ ./run.sh mh 1000

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
