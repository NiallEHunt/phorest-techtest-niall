# Fruit Machine

A virtual slot machine. Built for the [Phorest take-home programming assignment](docs/candidate-brief-fruit-machine-take-home-1-1-.pdf)

This project is built using Kotlin, Gradle and JDK 25. You will need Java 17 or newer installed to run the project. The Gradle wrapper downloads the correct Gradle version and JDK versions automatically if needed.

Run the tests with:
```sh
./gradlew test
```

Run the service with:
```sh
./gradlew :fruit-machine-service:bootRun
```
The service starts on `http://localhost:8080`. Machines are stored in memory, so they are lost when the service stops.

## Project structure
- `fruit-machine-core`: the game logic. Pure Kotlin with no Spring dependency. It takes a machine and returns the result of a play along with the updated machine.
- `fruit-machine-service`: the Spring Boot REST service. It validates requests, stores the machines and calls into the core.
- `investigations`: performance benchmarks I used to decide how to represent colours. Not part of the service. See [investigations/README.md](investigations/README.md).

The key decisions I made, and why, are in the [decision log](docs/decision_log.md).

## Endpoints
All amounts are in the minor units of an ISO 4217 currency (e.g. `100` EUR is €1.00).

### Create a machine
`POST /machines`
```json
{
  "slotCount": 4,
  "colours": ["black", "white", "green", "yellow"],
  "adjacentSlotsForSmallPrize": 2,
  "playCost": { "minorUnits": 100, "currency": "EUR" },
  "startingFloat": { "minorUnits": 5000, "currency": "EUR" }
}
```
Returns `201 Created` with a `Location` header pointing at the new machine:
```json
{
  "id": "78616602-65a0-445d-8ef9-dea625b796cc",
  "slotCount": 4,
  "colours": ["black", "white", "green", "yellow"],
  "adjacentSlotsForSmallPrize": 2,
  "playCost": { "minorUnits": 100, "currency": "EUR" },
  "float": { "minorUnits": 5000, "currency": "EUR" }
}
```

### Read a machine
`GET /machines/{id}`

Returns `200 OK` with the same body as creating a machine, including the current float.

### Play a machine
`POST /machines/{id}/play`

Returns `200 OK` with the slots that were spun, the outcome (`JACKPOT`, `FULL_HOUSE`, `SMALL_PRIZE` or `NO_PRIZE`), what was paid out, any free plays credited and the machine's new float:
```json
{
  "slots": ["black", "black", "green", "green"],
  "outcome": "SMALL_PRIZE",
  "payout": { "minorUnits": 500, "currency": "EUR" },
  "freePlaysCredited": 0,
  "float": { "minorUnits": 4600, "currency": "EUR" }
}
```

### Errors
Errors use the [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) problem details format.
- `404 Not Found` when no machine exists with the given id.
- `400 Bad Request` when a machine breaks a game rule (e.g. fewer than 3 slots, duplicate colours), when it goes over a service limit, or when the request is malformed. Service limit failures list each invalid field:
```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid request content.",
  "instance": "/machines",
  "errors": [{ "field": "slotCount", "message": "must be less than or equal to 10000" }]
}
```

## What I left out
- **Persistence.** Machines live in memory. The repository is an interface with a contract test, so a database implementation could be swapped in. Across multiple instances, I'd use optimistic locking in the database to keep plays on the same machine atomic.
- **Players and redeeming free plays.** Free plays are reported in the play result but not stored anywhere. There are no players or accounts to hold them.
- **Reconfiguring a machine.** A machine's config is fixed once it's created. To change it, you create a new machine.
- **Configurable prize rules.** The rules are hard-coded in a single pass over the slots. See the decision log for the trade-off.
