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

## 4. Part 2

### Money
While not explicitly called out in the assignment brief, I have chosen to represent all monetary values with a `Money` type. This will be composed of the amount and the currency. The amount will be a long and will be in the minor units of the currency. This will ensure we don't run into floating point rounding/precision errors. This is perhaps verging on overkill for this assignment, but I have been bitten in the past and know that internationalising money in critical systems can be quite difficult, so I think it's important to have this baked in from the beginning. 

I considered using existing libraries for this Money type, but I didn't find one that seems to be considered an industry standard. From my experience, a lot of companies have their own handrolled versions like this, especially as how a company treats money is often quite specific to their business.

For formatting, the locale specific formatting is a client concern. The REST endpoint will return the amount and the currency and the client will be responsible for handling locale specific formatting.

### Game logic
It's not explicitly mentioned in the brief but I am assuming the price of the single play goes into the machine's prize pot before the prizes are won.

In a similar vein, I am assuming only one rule will apply for any given spin. The rules will take effect in order (e.g. every jackpot will also qualify for the small prize, however, the jackpot will always take precedence over the small prize).

With these assumptions, a small prize could be worth more to the player than a jackpot if the pot is low. The jackpot and full house prizes are tied to the amount in the pot while the small prize is always 5 times the cost to play. This also means that only the small prize can ever be worth more than is in the pot and therefore the only rule that can win the player free spins. The brief says that the free plays rule "does not apply to a jackpot", which in my interpretation is true becuase it is not possible to ever win more than the pot. This is how I will implement this task but I would want to confirm all of these assumptions with the product team to ensure this is the intended behaviour.

