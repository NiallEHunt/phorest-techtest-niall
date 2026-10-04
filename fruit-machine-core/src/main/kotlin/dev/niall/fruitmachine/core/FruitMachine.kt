package dev.niall.fruitmachine.core

import kotlin.random.Random

enum class Colour { BLACK, WHITE, GREEN, YELLOW }
enum class Outcome { JACKPOT, NO_PRIZE }

data class PlayResult(val slots: List<Colour>, val outcome: Outcome)

fun computeOutcomeOf(slots: List<Colour>): Outcome =
    if (slots.distinct().size == 1) Outcome.JACKPOT else Outcome.NO_PRIZE

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
