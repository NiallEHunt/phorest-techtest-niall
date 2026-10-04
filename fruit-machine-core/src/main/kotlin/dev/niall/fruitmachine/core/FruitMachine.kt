package dev.niall.fruitmachine.core

import kotlin.random.Random

enum class Colour { BLACK, WHITE, GREEN, YELLOW }
enum class Outcome { JACKPOT, FULL_HOUSE, SMALL_PRIZE, NO_PRIZE }

data class Payout(val amount: Money, val freePlays: Long)
data class PlayResult(val slots: List<Colour>, val outcome: Outcome, val payout: Payout, val float: Money)

private const val SMALL_PRIZE_MULTIPLIER = 5

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


fun computePayoutOf(outcome: Outcome, float: Money, playCost: Money): Payout {
    val zero = Money(0, float.currency)
    require(playCost > zero) { "Play cost must be greater than zero" }

    val prize = when (outcome) {
        Outcome.JACKPOT -> float
        // Rounds down, so the machine keeps any odd cent.
        Outcome.FULL_HOUSE -> float / 2
        Outcome.SMALL_PRIZE -> playCost * SMALL_PRIZE_MULTIPLIER
        Outcome.NO_PRIZE -> zero
    }

    if (prize <= float) {
        return Payout(amount = prize, freePlays = 0)
    }

    // The prize owed to the player is greater than the machine's float. The entire float
    // is given as a prize and the remainder is given as free plays. Free plays round down
    // (i.e. a player is not given a free play if their shortfall doesn't cover the full cost
    // of a play).
    val shortfall = prize - float
    return Payout(
        amount = float,
        freePlays = shortfall / playCost
    )
}

class FruitMachine(private val random: Random) {
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
