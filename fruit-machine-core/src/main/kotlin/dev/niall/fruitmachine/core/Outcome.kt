package dev.niall.fruitmachine.core

enum class Outcome { JACKPOT, FULL_HOUSE, SMALL_PRIZE, NO_PRIZE }

private fun allSame(slots: List<Colour>): Boolean = slots.distinct().size == 1
private fun allDifferent(slots: List<Colour>): Boolean = slots.distinct().size == slots.size

private fun hasAdjacentMatches(slots: List<Colour>, count: Int): Boolean {
    var run = 1
    for (i in 1 until slots.size) {
        run = if (slots[i] == slots[i - 1]) run + 1 else 1
        if (run >= count) return true
    }
    return false
}

fun computeOutcomeOf(slots: List<Colour>, adjacentSlotsForSmallPrize: Int): Outcome =
    when {
        allSame(slots) -> Outcome.JACKPOT
        allDifferent(slots) -> Outcome.FULL_HOUSE
        hasAdjacentMatches(slots, adjacentSlotsForSmallPrize) -> Outcome.SMALL_PRIZE
        else -> Outcome.NO_PRIZE
    }
