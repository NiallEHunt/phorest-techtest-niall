package dev.niall.fruitmachine.investigations

import dev.niall.fruitmachine.core.Outcome

// The improved evaluation algorithm suggested by the first round of results: one pass over
// the row that checks all three rules together and stops as soon as the outcome is certain.
// The core calls distinct() twice over the whole row instead.
//
// Generic, so the same code runs on the core's Colour and on PlainColour. A List<Colour>
// already holds boxed Colours, and a generic function uses those boxes as they are, so
// adding one to the set doesn't create a new wrapper.
fun <T> computeOutcomeOfSinglePass(slots: List<T>, adjacentSlotsForSmallPrize: Int): Outcome {
    val seen = HashSet<T>()
    var allSame = true
    var allDifferent = true
    var run = 1
    var hasAdjacentMatches = false

    for (i in slots.indices) {
        val slot = slots[i]
        if (i > 0) {
            if (slot == slots[i - 1]) {
                run++
            } else {
                allSame = false
                run = 1
            }
            if (run >= adjacentSlotsForSmallPrize) hasAdjacentMatches = true
        }

        // Once a colour repeats the row can't be a full house, so the set is no longer needed.
        if (allDifferent && !seen.add(slot)) allDifferent = false

        // Neither a jackpot nor a full house, and a small prize is already certain.
        if (!allSame && !allDifferent && hasAdjacentMatches) return Outcome.SMALL_PRIZE
    }

    return outcomeOf(allSame, allDifferent, hasAdjacentMatches)
}

// The same single-pass algorithm on a primitive Spin, counting colours in a BooleanArray
// instead of a hash set. Nothing is boxed or hashed, but the BooleanArray is as big as the
// colour list, so it costs more as the number of colours grows.
fun computeOutcomeOfSinglePass(slots: Spin, colourCount: Int, adjacentSlotsForSmallPrize: Int): Outcome {
    val seen = BooleanArray(colourCount)
    var allSame = true
    var allDifferent = true
    var run = 1
    var hasAdjacentMatches = false

    for (i in 0 until slots.size) {
        val slot = slots[i]
        if (i > 0) {
            if (slot == slots[i - 1]) {
                run++
            } else {
                allSame = false
                run = 1
            }
            if (run >= adjacentSlotsForSmallPrize) hasAdjacentMatches = true
        }

        if (allDifferent) {
            if (seen[slot.index]) allDifferent = false else seen[slot.index] = true
        }

        if (!allSame && !allDifferent && hasAdjacentMatches) return Outcome.SMALL_PRIZE
    }

    return outcomeOf(allSame, allDifferent, hasAdjacentMatches)
}

private fun outcomeOf(allSame: Boolean, allDifferent: Boolean, hasAdjacentMatches: Boolean): Outcome =
    when {
        allSame -> Outcome.JACKPOT
        allDifferent -> Outcome.FULL_HOUSE
        hasAdjacentMatches -> Outcome.SMALL_PRIZE
        else -> Outcome.NO_PRIZE
    }
