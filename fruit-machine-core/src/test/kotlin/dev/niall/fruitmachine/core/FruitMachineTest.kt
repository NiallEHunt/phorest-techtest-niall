package dev.niall.fruitmachine.core

import dev.niall.fruitmachine.core.Colour.BLACK
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
