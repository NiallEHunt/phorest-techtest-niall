package dev.niall.fruitmachine.investigations

import dev.niall.fruitmachine.core.Outcome

// Int-based alternatives to the core's String-backed Colour, written only so they can be
// benchmarked against it. Each mirrors the logic in fruit-machine-core's Outcome.kt.

// A colour is an index into the config's colour list, held in a List<Int>.
// Uses exactly the same algorithm as the core.
fun computeOutcomeOfIntList(slots: List<Int>, adjacentSlotsForSmallPrize: Int): Outcome =
    when {
        slots.distinct().size == 1 -> Outcome.JACKPOT
        slots.distinct().size == slots.size -> Outcome.FULL_HOUSE
        hasAdjacentMatches(slots, adjacentSlotsForSmallPrize) -> Outcome.SMALL_PRIZE
        else -> Outcome.NO_PRIZE
    }

// A colour is an index held in a primitive IntArray, still using the same algorithm.
// Note that IntArray.distinct() returns a List<Int>, so it still boxes during evaluation.
fun computeOutcomeOfIntArray(slots: IntArray, adjacentSlotsForSmallPrize: Int): Outcome =
    when {
        slots.distinct().size == 1 -> Outcome.JACKPOT
        slots.distinct().size == slots.size -> Outcome.FULL_HOUSE
        hasAdjacentMatches(slots, adjacentSlotsForSmallPrize) -> Outcome.SMALL_PRIZE
        else -> Outcome.NO_PRIZE
    }

// The best case for ints: a primitive IntArray and a primitive algorithm. Distinct colours are
// counted with a BooleanArray indexed by colour, so nothing is boxed and nothing is hashed.
fun computeOutcomeOfIntArrayPrimitive(
    slots: IntArray,
    colourCount: Int,
    adjacentSlotsForSmallPrize: Int
): Outcome {
    val seen = BooleanArray(colourCount)
    var distinctColours = 0
    for (colour in slots) {
        if (!seen[colour]) {
            seen[colour] = true
            distinctColours++
        }
    }

    return when {
        distinctColours == 1 -> Outcome.JACKPOT
        distinctColours == slots.size -> Outcome.FULL_HOUSE
        hasAdjacentMatches(slots, adjacentSlotsForSmallPrize) -> Outcome.SMALL_PRIZE
        else -> Outcome.NO_PRIZE
    }
}

private fun hasAdjacentMatches(slots: List<Int>, count: Int): Boolean {
    var run = 1
    for (i in 1 until slots.size) {
        run = if (slots[i] == slots[i - 1]) run + 1 else 1
        if (run >= count) return true
    }
    return false
}

private fun hasAdjacentMatches(slots: IntArray, count: Int): Boolean {
    var run = 1
    for (i in 1 until slots.size) {
        run = if (slots[i] == slots[i - 1]) run + 1 else 1
        if (run >= count) return true
    }
    return false
}
