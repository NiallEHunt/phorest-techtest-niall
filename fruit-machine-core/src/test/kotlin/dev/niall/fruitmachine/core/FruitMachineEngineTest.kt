package dev.niall.fruitmachine.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FruitMachineEngineTest {
    @Test
    fun `a play spins the configured number of slots using only the configured colours`() {
        val config = FruitMachineConfig(
            slotCount = 10,
            colours = listOf(BLACK, WHITE),
            playCost = PLAY_COST
        )

        val result = FruitMachineEngine(Random(10)).play(FruitMachine(config, euro(1000)))

        assertEquals(10, result.slots.size)
        assertTrue(result.slots.all { it in config.colours })
    }

    @Test
    fun `a play with all the same colours wins a jackpot`() {
        // Fix the random to always return 0. This means every slot will spin the same colour (the first in our colours list)
        val fixedRandom = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
        }

        val result = FruitMachineEngine(fixedRandom).play(FruitMachine(DEFAULT_CONFIG, euro(1000)))

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
