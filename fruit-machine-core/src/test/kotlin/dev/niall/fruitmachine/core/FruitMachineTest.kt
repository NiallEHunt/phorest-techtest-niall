package dev.niall.fruitmachine.core

import dev.niall.fruitmachine.core.Colour.BLACK
import dev.niall.fruitmachine.core.Colour.GREEN
import dev.niall.fruitmachine.core.Colour.WHITE
import dev.niall.fruitmachine.core.Colour.YELLOW
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class OutcomeTest {
    @Test
    fun `all slots the same colour is a jackpot`() {
        assertEquals(Outcome.JACKPOT, computeOutcomeOf(listOf(GREEN, GREEN, GREEN, GREEN)))
    }

    @Test
    fun `one different colour slot is not a jackpot`() {
        assertEquals(Outcome.NO_PRIZE, computeOutcomeOf(listOf(GREEN, WHITE, WHITE, WHITE)))
    }

    @Test
    fun `all different colour slots is not a jackpot`() {
        assertEquals(Outcome.NO_PRIZE, computeOutcomeOf(listOf(GREEN, WHITE, BLACK, YELLOW)))
    }
}

class FruitMachineTest {
    @Test
    fun `a play spins four slots`() {
        val result = FruitMachine(Random(10)).play()

        assertEquals(4, result.slots.size)
    }

    @Test
    fun `a play with all the same colours wins a jackpot`() {
        // Fix the random to always return 0. This means every slot will spin the same colour (the first in our colours list)
        val fixedRandom = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
        }

        val result = FruitMachine(fixedRandom).play()

        assertEquals(PlayResult(List(4) { Colour.BLACK }, Outcome.JACKPOT), result)
    }
}
