package dev.niall.fruitmachine.core

import kotlin.test.Test
import kotlin.test.assertEquals

class OutcomeTest {
    @Test
    fun `all slots the same colour is a jackpot`() {
        assertEquals(
            Outcome.JACKPOT,
            computeOutcomeOf(listOf(GREEN, GREEN, GREEN, GREEN, GREEN, GREEN, GREEN))
        )
    }

    @Test
    fun `all different colours is a full house`() {
        assertEquals(
            Outcome.FULL_HOUSE,
            computeOutcomeOf(listOf(GREEN, WHITE, BLACK, YELLOW, RED, BLUE, PURPLE))
        )
    }

    @Test
    fun `a matching pair is a small prize`() {
        assertEquals(
            Outcome.SMALL_PRIZE,
            computeOutcomeOf(listOf(GREEN, GREEN, WHITE, BLACK, YELLOW, RED, BLUE))
        )
        assertEquals(
            Outcome.SMALL_PRIZE,
            computeOutcomeOf(listOf(WHITE, BLACK, YELLOW, GREEN, GREEN, RED, BLUE))
        )
        assertEquals(
            Outcome.SMALL_PRIZE,
            computeOutcomeOf(listOf(WHITE, BLACK, YELLOW, RED, BLUE, GREEN, GREEN))
        )
    }

    @Test
    fun `greater than 2 adjacent matching colours is still a small prize`() {
        assertEquals(
            Outcome.SMALL_PRIZE,
            computeOutcomeOf(listOf(GREEN, BLACK, WHITE, WHITE, WHITE, YELLOW, RED))
        )
    }

    @Test
    fun `repeated colours that are not adjacent win no prize`() {
        assertEquals(
            Outcome.NO_PRIZE,
            computeOutcomeOf(listOf(BLACK, WHITE, BLACK, WHITE, BLACK, WHITE, BLACK))
        )
    }
}
