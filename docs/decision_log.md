# Decision log
This is a log of the key decisions I've made while working on this assignment. This includes architectural decisions, assumptions and where the brief is ambiguous I've included my choice here.

## 1. Technology used
The brief specifies using Spring Boot for the REST service, so no choice here. It does, however, allow either Java or Kotlin. I chose to use Kotlin as it's what I use day to day and I'm much more comfortable with it. I am using Gradle for the same reason and because I find Kotlin DSL build scripts easier to follow than Maven's XML. Although it may not be applicable to this assignment, I find Kotlin's coroutine support very helpful for adding concurrency to large systems and I appreciate its null safety by design.

I chose to use the latest LTS for the JDK (25), Gradle, Kotlin and Spring Boot.

## 2. Architecture 
As is hinted at in the brief, I have chosen to split the game logic and web layer into two modules.

The core game logic will be stateless and work on immutable data. The spin function will take in the state of the machine and the rules and return the new state. This along with injected randomness will allow us to easily test the logic and ensure it is deterministic. The service layer will own the persistence of the machine states. Decoupling compute and storage like this will allow us to scale both independently. This introduces a concurrency risk when plays on the same machine run at the same time. Within a single instance, this will be handled with atomic per-machine updates. Coordinating across multiple instances is out of scope for this assignment (we would likely solve this with some form of optimistic locking in the persistence layer).

## 3. Part 1
Part 1 says we should return the "outcome of the play". I have chosen to return both the outcome (jackpot vs no jackpot) and the slots that were spun. As a player of this game I would imagine you would want to see the actual results of the spin to verify the outcome is what you expect.

In part 1, I have chosen to represent the colours and the outcomes as enums. In future parts we will need to expand on this to be able to generalise to an arbitrary list of colours. However, focusing on this part alone we do not need anything other than an enum yet.

In testing, I have separated out the tests for the outcome of a set of slots vs the actual play call itself. This allows us to test the jackpot rule independently and deterministically. We can then test the play function with a fixed random as a sort of integration test.
