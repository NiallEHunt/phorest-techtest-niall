package dev.niall.fruitmachine.core

data class Payout(val amount: Money, val freePlays: Long)

private const val SMALL_PRIZE_MULTIPLIER = 5

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
