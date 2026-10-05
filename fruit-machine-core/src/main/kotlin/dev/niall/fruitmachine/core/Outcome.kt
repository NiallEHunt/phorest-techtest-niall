package dev.niall.fruitmachine.core

enum class Outcome { JACKPOT, FULL_HOUSE, SMALL_PRIZE, NO_PRIZE }

fun computeOutcomeOf(slots: List<Colour>, adjacentSlotsForSmallPrize: Int): Outcome {
    val seen = HashSet<Colour>()
    var allSame = true
    var allDifferent = true
    var run = 1
    var hasAdjacentMatches = false

    for (i in slots.indices) {
        if (i > 0) {
            if (slots[i] == slots[i - 1]) {
                run++
            } else {
                allSame = false
                run = 1
            }
            if (run >= adjacentSlotsForSmallPrize) hasAdjacentMatches = true
        }

        // Once a colour repeats, the slots can't be a full house, so there's no need to track colours.
        if (allDifferent && !seen.add(slots[i])) allDifferent = false

        // Neither a jackpot nor a full house is possible any more, and the threshold for the small prize has already been met.
        if (!allSame && !allDifferent && hasAdjacentMatches) return Outcome.SMALL_PRIZE
    }

    return when {
        allSame -> Outcome.JACKPOT
        allDifferent -> Outcome.FULL_HOUSE
        hasAdjacentMatches -> Outcome.SMALL_PRIZE
        else -> Outcome.NO_PRIZE
    }
}
