package dev.niall.fruitmachine.core

import kotlin.random.Random

enum class Colour { BLACK, WHITE, GREEN, YELLOW }
enum class Outcome { JACKPOT, FULL_HOUSE, SMALL_PRIZE, NO_PRIZE }

data class PlayResult(val slots: List<Colour>, val outcome: Outcome)

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

class FruitMachine(private val random: Random) {
    companion object {
        const val SLOT_COUNT = 4
    }

    private fun spin(): List<Colour> = List(SLOT_COUNT) { Colour.entries.random(random) }
    
    fun play(): PlayResult {
        val slots = spin()
        return PlayResult(slots, computeOutcomeOf(slots))
    }
}
