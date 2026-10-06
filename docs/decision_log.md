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

With these assumptions, a small prize could be worth more to the player than a jackpot if the pot is low. The jackpot and full house prizes are tied to the amount in the pot while the small prize is always 5 times the cost to play. This also means that only the small prize can ever be worth more than is in the pot and therefore the only rule that can win the player free spins. The brief says that the free plays rule "does not apply to a jackpot", which in my interpretation is true because it is not possible to ever win more than the pot. This is how I will implement this task but I would want to confirm all of these assumptions with the product team to ensure this is the intended behaviour.

### Prizes
I have decided that rounding always favours the machine. The machine keeps the odd cent when halving the float to give as a prize for a full house. When converting a shortfall into free plays, only whole free plays are credited and the remainder is kept by the machine (e.g. a shortfall of €14 and a price to play of €5 would net the user only 2 free spins). This would be a decision I would have to confirm with the product team. For the purposes of this exercise, I am assuming the business would want to retain these small advantages.

## 5. Part 3
### Colour representation
Up until this point, I have represented colours using an enum because we had a known set of colours that we had to support. However, in this part we need to generalise to be able to support arbitrary lists of colours. My initial thought was to use a value class called `Colour` that wrapped a string representation of that colour. A value class is a compile-time wrapper around a single value, meaning at runtime, in most cases, the bytecode will just use the wrapped string instead of allocating a full `Colour` object. But crucially, at compile time we treat it as a `Colour` and have the type safety of the compiler to help.

I was worried that the wrapped string's comparisons would be significantly slower than if we used an integer to represent the colours. Because the colour representation and comparisons are core to the scalability of this system, I used AI to help explore this decision more thoroughly. I leaned on AI to create a performance testing framework (this exists in the investigations module, and `investigations/README.md` has the full results) to test my thinking and some other alternatives. I tested a value class wrapping a string, a data class wrapping a string, a value class wrapping an int, a list of ints and an IntArray. The thinking would be that the integers represented the index of the configured colours for that machine. The caller would then need to map that index back into the actual colour.

I had an inkling that the exact representation would have an impact on the performance of the system but that it might not be a large enough difference to warrant losing the readability of the code. This is always a tradeoff worth investigating further. The AI-aided performance investigations backed up my thinking. While the IntArray version of the colour generally performed the best (it had the least memory allocated and usually completed the fastest), it was a rather negligible difference when compared to the cost of a HTTP request, the network calls and database retrieval that would be required in a real world service. A 4-slot play took well under a microsecond in every representation. The integer version wasn't always faster: with 100,000 colours, a 4-slot play took about 13 µs and allocated about 98 KB, while the strings took about 0.3 µs. Even when playing excessively large games or playing many concurrent games, the difference was not enough in my opinion to warrant the loss in readability and maintainability. In a real world situation I would back this up with further performance testing and then evaluation in the production environment.

The investigation also taught me something I hadn't expected. When a value class is stored in a collection, it always has to be wrapped in an object rather than using the wrapped value directly, so every slot in a spin allocated a new `Colour`. The benchmarks also highlighted that the biggest performance gain to be made was in the evaluation algorithm. This is something I thought of in the beginning but deferred due to the limited scale. The benchmarks highlighted the importance of doing every rule check in a single pass and stopping as soon as the outcome is certain. In the benchmarks this led to about a 2x speedup on the original 4-slot game, and orders of magnitude on large machines (evaluating 1,000,000 slots went from 48 ms to about 80 ns).

My decision after this investigation was to use a data class rather than a value class, while keeping the wrapped value as a string. With a data class, the slots in a spin reference the machine's existing `Colour` instances instead of each creating a new wrapper, so less memory is allocated per spin, and a string is easier for engineers to read. I also decided to change the outcome evaluation to check every rule in a single pass, stopping as soon as the outcome is certain, so that it stays efficient as the number of slots grows.

### Generalising
It was straightforward in this architecture to generalise the machines. The `FruitMachineEngine` is given a machine that is preconfigured. The caller of this engine is in charge of storing that configuration and float. This will be tackled in part 4. As talked about in the architecture section, I chose to keep the machine configuration and data separated from the logic. This lets us build pure functions that operate on the given data and return the output. This helps with testing and separation of concerns.

There was a trade-off I had to make with the game rules. Based on the performance investigations in the colour representation section above, I decided to perform all of the rule checks in one pass through the slots and exit early. With this decision, adding additional rules becomes a bit of a headache. We would have to add the new logic into the existing evaluator and the complexity would continue to grow. We could have rules defined as data/config similarly to the machine config, which would make it a lot more extensible. The engine would take in the rules and how to evaluate them and perform the evaluation. However, for this exercise we don't have a requirement to add additional rules yet. I decided it was better to not generalise early. We may never need to add additional rules, in which case that extra complexity would be wasted, and keeping the single pass with an early exit would take more machinery. We would have to revisit this decision when the need for additional rules comes up.

## 6. Part 4
### API design
The brief talks about "the machine" but I have chosen to support many machines at once, each with its own id, rather than a single global machine that gets reconfigured. Creating a machine is a `POST /machines`, which returns a `201 Created` with the new machine's id and a `Location` header. Its state is read with `GET /machines/{id}`, and it's played with `POST /machines/{id}/play`. The ids are random UUIDs.

I have interpreted "configure a machine" as choosing the config when the machine is created. Once created, a machine's config can't be changed. Changing the slots, colours or play cost of a machine that already has a float that was built up under different rules seemed confusing for players and would raise questions I can't answer without the product team (e.g. what happens to free plays credited at the old play cost).

The play response returns the slots that were spun, the outcome, the payout, any free plays credited and the machine's new float. This follows on from part 1. I am assuming a player would want to see the actual spin, not just be told whether they won. All money is returned as minor units and a currency code, and formatting is left to the client (see Money in part 2).

The request and response types are DTOs in the service module rather than the core's types. This keeps the JSON shape of the API separate from the game logic, so either can change without breaking the other.

### Free plays
Free plays credited by a shortfall are reported in the play result, but they aren't stored on the machine. There is no concept of a player or account in this assignment, so there is nothing to own the credit. The caller (e.g. a client tracking a player's session) would be responsible for holding and redeeming them.

I considered storing them on the machine, like credits on a physical machine. However, if free plays were then used automatically, a machine with an empty float could get stuck. A free play adds no stake to the float, so every small prize on an empty float would credit 5 more free plays. In my opinion redeeming free plays properly needs players, which is a bigger change than this assignment calls for.

### State
The machines are stored behind a `MachineRepository` interface. The only implementation is in memory, backed by a `ConcurrentHashMap`. The service doesn't know how the machines are stored, so a database could be swapped in without touching the game logic or the API.

As mentioned in the architecture section, two plays on the same machine at the same time must not both start from the same float. The repository's `update` is atomic per machine, and the guarantees an implementation must provide are written down on the interface. A shared contract test checks them, including a concurrency test that fails if the update isn't atomic. Any future implementation (e.g. a database using optimistic locking) can run the same test. Plays on different machines don't block each other.

### Validation
I have split validation into two kinds. The game rules (e.g. at least 3 slots, unique colours, a play cost above zero) stay in the core as `require` checks, because they define what a valid machine is no matter how it's created. The service adds its own limits on top using Bean Validation: at most 10,000 slots, 1,000 colours and amounts of 10^12 minor units. These aren't game rules. They stop a single request from creating a machine that is expensive to play, and keep amounts far enough from the limit of a `Long` that a play can't overflow.

### Error handling
All errors use the RFC 9457 problem details format, including Spring's own errors like malformed JSON, so a client only has to handle one error shape. When a request breaks a service limit, the response lists each invalid field rather than just saying the request was invalid.

The core reports broken game rules with `IllegalArgumentException`. Rather than turning every `IllegalArgumentException` into a 400, only the ones thrown while building a machine from a request are wrapped in an `InvalidMachineConfigException` and returned as a 400. Any other `IllegalArgumentException` is a bug in our code, so it should still be a 500 and not be blamed on the caller.

There is one known gap. The float can still overflow after millions of plays at the maximum play cost without a payout. `Money` uses exact arithmetic, so this throws rather than silently wrapping around, the play returns a 500 and the machine is left unchanged. I don't think this is worth handling for this assignment.

### Randomness
The service uses `SecureRandom` for spins. This is a game played for money, so it shouldn't be possible to predict the next spin from previous ones. The core still takes the random as a parameter, so tests can use a fixed one.
