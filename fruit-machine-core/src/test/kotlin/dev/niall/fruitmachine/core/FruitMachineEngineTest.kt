package dev.niall.fruitmachine.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FruitMachineEngineTest {
    private val fruitMachine = FruitMachine(DEFAULT_CONFIG, euro(1000))

    @Test
    fun `a play spins the configured number of slots`() {
        val result = FruitMachineEngine(Random(10)).play(fruitMachine)

        assertEquals(4, result.slots.size)
    }

    @Test
    fun `a play with all the same colours wins a jackpot`() {
        // Fix the random to always return 0. This means every slot will spin the same colour (the first in our colours list)
        val fixedRandom = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
        }

        val result = FruitMachineEngine(fixedRandom).play(fruitMachine)

        assertEquals(
            PlayResult(
                slots = List(4) { BLACK },
                outcome = Outcome.JACKPOT,
                payout = Payout(amount = euro(1100), freePlays = 0),
                machine = FruitMachine(DEFAULT_CONFIG, euro(0))
            ),
            result
        )
    }
}
