package dev.niall.fruitmachine.investigations

import dev.niall.fruitmachine.core.Outcome

// Frozen copies of the core's colour type and evaluation algorithm as they were when this
// investigation ran. As a result of it, the core has since switched to a data class Colour and
// a single-pass evaluation. The benchmarks use these copies so that re-running them still
// measures what README.md describes.

// The core's original Colour.
@JvmInline
value class ValueColour(val name: String)

// The core's original computeOutcomeOf: distinct() twice over the whole row, then the run check.
fun computeOutcomeOfOriginal(slots: List<ValueColour>, adjacentSlotsForSmallPrize: Int): Outcome =
    when {
        slots.distinct().size == 1 -> Outcome.JACKPOT
        slots.distinct().size == slots.size -> Outcome.FULL_HOUSE
        hasAdjacentMatches(slots, adjacentSlotsForSmallPrize) -> Outcome.SMALL_PRIZE
        else -> Outcome.NO_PRIZE
    }

private fun hasAdjacentMatches(slots: List<ValueColour>, count: Int): Boolean {
    var run = 1
    for (i in 1 until slots.size) {
        run = if (slots[i] == slots[i - 1]) run + 1 else 1
        if (run >= count) return true
    }
    return false
}
