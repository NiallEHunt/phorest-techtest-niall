package dev.niall.fruitmachine.core

import kotlin.random.Random

data class PlayResult(val slots: List<Colour>, val outcome: Outcome, val payout: Payout, val machine: FruitMachine)

class FruitMachineEngine(private val random: Random) {
    private fun spin(config: FruitMachineConfig): List<Colour> =
        List(config.slotCount) { config.colours.random(random) }
    
    fun play(machine: FruitMachine): PlayResult {
        val slots = spin(machine.config)
        val outcome = computeOutcomeOf(slots)

        // The play cost goes into the float before any prize is paid out.
        val available = machine.float + machine.config.playCost
        val payout = computePayoutOf(outcome, available, machine.config.playCost)

        return PlayResult(slots, outcome, payout, machine.copy(float = available - payout.amount))
    }
}
