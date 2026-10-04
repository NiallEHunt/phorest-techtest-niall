package dev.niall.fruitmachine.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
