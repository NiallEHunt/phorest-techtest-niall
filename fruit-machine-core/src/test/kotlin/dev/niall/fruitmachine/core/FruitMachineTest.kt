package dev.niall.fruitmachine.core

import dev.niall.fruitmachine.core.Colour.BLACK
import dev.niall.fruitmachine.core.Colour.GREEN
import dev.niall.fruitmachine.core.Colour.WHITE
import dev.niall.fruitmachine.core.Colour.YELLOW
import java.util.Currency
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private val EUR = Currency.getInstance("EUR")
private fun euro(minorUnits: Long) = Money(minorUnits, EUR)
private val PLAY_COST = euro(100)

class OutcomeTest {
    @Test
    fun `all slots the same colour is a jackpot`() {
        assertEquals(Outcome.JACKPOT, computeOutcomeOf(listOf(GREEN, GREEN, GREEN, GREEN)))
    }

    @Test
    fun `all different colours is a full house`() {
        assertEquals(Outcome.FULL_HOUSE, computeOutcomeOf(listOf(GREEN, WHITE, BLACK, YELLOW)))
    }

    @Test
    fun `a matching pair is a small prize`() {
        assertEquals(Outcome.SMALL_PRIZE, computeOutcomeOf(listOf(GREEN, GREEN, WHITE, BLACK)))
        assertEquals(Outcome.SMALL_PRIZE, computeOutcomeOf(listOf(WHITE, GREEN, GREEN, BLACK)))
        assertEquals(Outcome.SMALL_PRIZE, computeOutcomeOf(listOf(WHITE, BLACK, GREEN, GREEN)))
    }

    @Test
    fun `greater than 2 matching colours is still a small prize`() {
        assertEquals(Outcome.SMALL_PRIZE, computeOutcomeOf(listOf(GREEN, WHITE, WHITE, WHITE)))
    }

    @Test
    fun `repeated colours that are not adjacent win no prize`() {
        assertEquals(Outcome.NO_PRIZE, computeOutcomeOf(listOf(BLACK, WHITE, BLACK, WHITE)))
    }
}

class PayoutTest {
    @Test
    fun `no prize pays nothing`() {
        assertEquals(
            Payout(amount = euro(0), freePlays = 0),
            computePayoutOf(Outcome.NO_PRIZE, euro(1100), PLAY_COST)
        )
    }

    @Test
    fun `a jackpot pays out the whole float`() {
        assertEquals(
            Payout(amount = euro(1100), freePlays = 0),
            computePayoutOf(Outcome.JACKPOT, euro(1100), PLAY_COST)
        )
    }

    @Test
    fun `a full house pays out half the float`() {
        assertEquals(
            Payout(amount = euro(550), freePlays = 0),
            computePayoutOf(Outcome.FULL_HOUSE, euro(1100), PLAY_COST)
        )
    }

    @Test
    fun `a full house on an odd float rounds down`() {
        assertEquals(
            Payout(amount = euro(550), freePlays = 0),
            computePayoutOf(Outcome.FULL_HOUSE, euro(1101), PLAY_COST)
        )
    }

    @Test
    fun `a small prize pays out five times the play cost`() {
        assertEquals(
            Payout(amount = euro(500), freePlays = 0),
            computePayoutOf(Outcome.SMALL_PRIZE, euro(1100), PLAY_COST)
        )
    }

    @Test
    fun `a small prize the float cannot cover pays out the float and credits the shortfall as free plays`() {
        assertEquals(
            Payout(amount = euro(300), freePlays = 2),
            computePayoutOf(Outcome.SMALL_PRIZE, euro(300), PLAY_COST)
        )
    }

    @Test
    fun `a shortfall that is not a whole number of plays rounds down`() {
        assertEquals(
            Payout(amount = euro(350), freePlays = 1),
            computePayoutOf(Outcome.SMALL_PRIZE, euro(350), PLAY_COST)
        )
    }

    @Test
    fun `a small prize from a float of only the play cost credits four free plays`() {
        assertEquals(
            Payout(amount = euro(100), freePlays = 4),
            computePayoutOf(Outcome.SMALL_PRIZE, euro(100), PLAY_COST)
        )
    }

    @Test
    fun `rejects a play cost of zero`() {
        assertFailsWith<IllegalArgumentException> {
            computePayoutOf(Outcome.NO_PRIZE, euro(1100), euro(0))
        }
    }
}

class FruitMachineTest {
    @Test
    fun `a play spins four slots`() {
        val result = FruitMachine(Random(10)).play(euro(1000), PLAY_COST)

        assertEquals(4, result.slots.size)
    }

    @Test
    fun `a play with all the same colours wins a jackpot`() {
        // Fix the random to always return 0. This means every slot will spin the same colour (the first in our colours list)
        val fixedRandom = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
        }

        val result = FruitMachine(fixedRandom).play(euro(1000), PLAY_COST)

        assertEquals(
            PlayResult(
                slots = List(4) { BLACK },
                outcome = Outcome.JACKPOT,
                payout = Payout(amount = euro(1100), freePlays = 0),
                float = euro(0)
            ),
            result
        )
    }

    @Test
    fun `rejects a negative float`() {
        assertFailsWith<IllegalArgumentException> {
            FruitMachine(Random(10)).play(euro(-1), PLAY_COST)
        }
    }
}
