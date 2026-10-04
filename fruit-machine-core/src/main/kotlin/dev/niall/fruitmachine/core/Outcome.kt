package dev.niall.fruitmachine.core

enum class Outcome { JACKPOT, FULL_HOUSE, SMALL_PRIZE, NO_PRIZE }

private fun allSame(slots: List<Colour>): Boolean = slots.distinct().size == 1
private fun allDifferent(slots: List<Colour>): Boolean = slots.distinct().size == slots.size
private fun hasAdjacentMatch(slots: List<Colour>): Boolean = (0 until slots.lastIndex).any { i -> slots[i] == slots[i + 1]}

fun computeOutcomeOf(slots: List<Colour>): Outcome =
    when {
        allSame(slots) -> Outcome.JACKPOT
        allDifferent(slots) -> Outcome.FULL_HOUSE
        hasAdjacentMatch(slots) -> Outcome.SMALL_PRIZE
        else -> Outcome.NO_PRIZE
    }
