package dev.niall.fruitmachine.core

import kotlin.random.Random

enum class Colour { BLACK, WHITE, GREEN, YELLOW }

data class PlayResult(val slots: List<Colour>, val outcome: Outcome, val payout: Payout, val float: Money)

class FruitMachineEngine(private val random: Random) {
    companion object {
        const val SLOT_COUNT = 4
    }

    private fun spin(): List<Colour> = List(SLOT_COUNT) { Colour.entries.random(random) }
    
    fun play(float: Money, playCost: Money): PlayResult {
        require(float >= Money(0, float.currency)) { "Float cannot be negative" }

        val slots = spin()
        val outcome = computeOutcomeOf(slots)

        // The play cost goes into the float before any prize is paid out.
        val available = float + playCost
        val payout = computePayoutOf(outcome, available, playCost)

        return PlayResult(slots, outcome, payout, available - payout.amount)
    }
}
