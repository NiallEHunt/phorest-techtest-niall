# Running notes
> [!NOTE]
> This markdown file is an unstructured log of my thinking as I tackle this problem. It is not intended to be easily parsable by anyone other than me and will mostly be used as a scratchpad and place for brain dumps. The project's README and decision_log will be the correct place to read my thinking in a more coherent and polished manner.

## First read through of assignment
> You are building a virtual Fruit Machine: a small, self-contained program (no external services or APIs required). It has slots that show colours when you spin, and it pays out prizes based on the result. Build it up in the three parts below. Each part builds on the previous one.
> 
> Build it as a Spring Boot service driven through REST endpoints (this is a backend role, so we want to see a little infrastructure around the game logic, not just a library). Java or Kotlin are both fine. Structure it as a real project you would be happy for a teammate to pick up.

- Build a slot machine
- Expose it as a REST service using Spring Boot
- 4 phases/parts:
    1. Core game logic
    2. Pay to win
    3. Generalise
    4. Make it a service
- Can use AI (it is encouraged). Log usage
- A proper README and decision log that is very much not this document
- Can use Java or Kotlin. Will use Kotlin as I prefer it. Will use latest LTS versions.

### Part 1
> The machine has four slots. Each slot can display one of four colours: black, white, green, or yellow.
> - A spin randomly selects a colour for each of the four slots.
> - Detect a jackpot: all four slots show the same colour.
> - Return the outcome of the play.

Build a core game logic module which will encapsulate the game and the game state (later). `FruitMachine` object that can be instantiated multiple times (this will be relevant in part 3)? Does it hold its own state? IMO no, it should be a pure logic function, the web server holds the state? Decoupled from the logic so that it can be stored separately. Come up with an answer here and explain.

### Part 2
> The machine holds a float (a starting sum of money). A single play has a fixed cost. After a spin, update and return the float according to these rules:
> - Jackpot (all four slots the same colour): pays out the entire float.
> - Full house (all four slots different colours): pays out half the float.
> - Small prize (two or more adjacent slots the same colour): pays out five times the cost of a single play.
> - If a prize is won but the float is too small to pay it in full, credit the player with free plays equal to the shortfall. This does not apply to a jackpot

Rules as written the last bullet point can only apply to the "small prize". That is the only prize that could be unpayable by the machine because it is 5 * x, where x is the price to play. Using y as our machine's float, if 5x > y, then they need free plays. The other two prizes are just y or 0.5y. They have no promise of more money. It is very possible that the "small prize" could be worth more than the jackpot in this ruleset. Also only one prize will be won and will be calculated in order (i.e. jackpot supersedes a small prize).

Assumption: price to play is added to float before spinning. 

### Part 3
> Generalise the machine so it is not hard-wired to four slots and four colours:
> - The number of slots is variable.
> - There can be hundreds of colours.
> - A small prize is now any k adjacent slots of the same colour, where k is an input (the original game is k = 2).
> 
> Aim for a solution that stays efficient as the number of slots grows.

The game logic itself is fairly easy to generalise. Especially if we have proper domain boundaries. The play/spin function itself can just take the number of slots, colours and k as parameters. The data for each individual machine will likely be stored as a data object outside of the game logic. The game logic will be a pure function. 

Do we want to generalise our ruleset as well? Make it easy to add additional rules? (e.g. if every odd slot is the same colour you get 10x or something). If we do this we may have to do each rule as an O(N) pass over the slots. So time complexity will be O(N) * number of rules. Which will still be O(N), but it would probably be more efficient to do one pass over the slots and evaluate all rules as you go, but doesn't generalise as well perhaps.

### Part 4
> Wrap the machine in a Spring Boot service and drive it through REST endpoints. The exact shape is your call, but we would expect something along the lines of:
> - Play the machine (spin, evaluate, update the float, return the outcome and any free plays credited).
> - Read the current machine state (float, configuration).
> - Configure a machine (slot count, colours, k, play cost, starting float).
> 
> Think about request/response design, input validation, error handling, and where the machine's state lives between calls. Keep the game logic cleanly separated from the web layer.

Will need to think through a bit what a frontend calling this would actually want/need. Will there be multiple machines on the go at once? Or when you configure a machine does it override the "current machine"? Do we have machine ids and when you check the state you specify the machine? Probably that one. Assuming no users/authentication for the assignment, but talk about how it would work maybe


## First thoughts on architecture
- Kotlin over Java because I am more familiar
- JDK 25
- Gradle
- Spring Boot

Core game logic module, API module. Core game logic is pure and stateless. The API/server is in charge of persisting machine state. Store in memory but abstract so underlying data source can change without any logic changes. Inject random so tests can be done on static seeds. MachineState data object. Decouple persisted data and pure game logic. 