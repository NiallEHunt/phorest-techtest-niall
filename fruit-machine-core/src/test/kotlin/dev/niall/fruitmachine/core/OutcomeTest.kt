package dev.niall.fruitmachine.core

import kotlin.test.Test
import kotlin.test.assertEquals

class OutcomeTest {
    @Test
    fun `all slots the same colour is a jackpot`() {
        assertEquals(
            Outcome.JACKPOT,
            computeOutcomeOf(listOf(GREEN, GREEN, GREEN, GREEN, GREEN, GREEN, GREEN), 2)
        )
    }

    @Test
    fun `all different colours is a full house`() {
        assertEquals(
            Outcome.FULL_HOUSE,
            computeOutcomeOf(listOf(GREEN, WHITE, BLACK, YELLOW, RED, BLUE, PURPLE), 2)
        )
    }

    @Test
    fun `repeated colours that are not adjacent win no prize`() {
        assertEquals(
            Outcome.NO_PRIZE,
            computeOutcomeOf(listOf(BLACK, WHITE, BLACK, WHITE, BLACK, WHITE, BLACK), 2)
        )
    }

    @Test
    fun `fewer adjacent matching slots than needed wins no prize`() {
        assertEquals(
            Outcome.NO_PRIZE,
            computeOutcomeOf(listOf(BLACK, WHITE, WHITE, GREEN, YELLOW, RED, BLACK), 3)
        )
    }

    @Test
    fun `exactly the needed adjacent matching slots is a small prize`() {
        assertEquals(
            Outcome.SMALL_PRIZE,
            computeOutcomeOf(listOf(WHITE, WHITE, WHITE, BLACK, GREEN, YELLOW, RED), 3)
        )
        assertEquals(
            Outcome.SMALL_PRIZE,
            computeOutcomeOf(listOf(BLACK, GREEN, WHITE, WHITE, WHITE, YELLOW, RED), 3)
        )
        assertEquals(
            Outcome.SMALL_PRIZE,
            computeOutcomeOf(listOf(BLACK, GREEN, YELLOW, RED, WHITE, WHITE, WHITE), 3)
        )
    }

    @Test
    fun `more adjacent matching slots than needed is still a small prize`() {
        assertEquals(
            Outcome.SMALL_PRIZE,
            computeOutcomeOf(listOf(BLACK, WHITE, WHITE, WHITE, WHITE, GREEN, YELLOW), 3)
        )
    }

    @Test
    fun `a small prize is impossible when the adjacent slots needed equals the slot count`() {
        // The only run as long as the row is every slot matching, which is a jackpot instead.
        assertEquals(
            Outcome.NO_PRIZE,
            computeOutcomeOf(listOf(GREEN, GREEN, GREEN, GREEN, GREEN, GREEN, BLACK), 7)
        )
    }
}
