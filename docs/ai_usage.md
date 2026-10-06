# AI usage log
I used AI (Claude Code) throughout this assignment. How much I leaned on it changed from part to part. For the core game logic I mostly used it as a sounding board and a code reviewer. For the performance investigation and the Spring Boot service I leaned on it a lot more, as those were the areas where it was most helpful to me. Throughout the project, I had claude code keep it's own draft of the decision log. I used this to remind myself of my usage for this document.

## Parts 1 and 2
Before asking Claude for anything, I wrote my own notes on the brief (`running_notes.md`). When I then asked it for a plan, we had reached the same main conclusions independently: a pure core, state held by the service and injected randomness. I also spotted that a small prize can be worth more than a jackpot, which its plan had missed.

From there I used it mostly to talk through designs and review my code in lieu of a human coworker.

It wrote a good share of the unit tests, which I reviewed and in places deleted or rewrote.

## Part 3
Generalising the game was the same as before, but I leaned on AI heavily for the performance investigation. I had a hunch about how representing colours as strings vs ints would affect performance, and wanted data rather than a guess. Claude built the benchmarks in the `investigations` module, ran them and wrote up the results. I drove the questions: which representations were missing, how far to scale and how it behaves with lots of small machines at once.

Its output needed checking here. It corrected several of its own overstated figures when I asked it to compare its write-up against the results, and found a mistake in its own AWS cost estimate.

## Part 4
I'm new to Spring Boot, so I relied on AI to help plumb the service together: setting up the module, the controller and DTOs, problem details for errors, Bean Validation, Actuator metrics and the Dockerfile. Generally I asked it to give me snippets to write myself, so I could learn how the pieces fit together. Later I had it implement more directly and reviewed what it did. It was useful for catching Spring specifics I wouldn't have known.

## What I took from it
AI was most useful to me as a reviewer and in the areas I was least familiar with: performance testing, unit tests and REST plumbing. It was also confidently wrong a number of times. Asking it to check its claims against real data (library usage, benchmark tables, the JDK itself) was one of the most effective things I did.
