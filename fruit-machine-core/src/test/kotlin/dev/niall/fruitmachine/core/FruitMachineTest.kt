package dev.niall.fruitmachine.core

import org.junit.jupiter.api.assertDoesNotThrow
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertFailsWith

class FruitMachineConfigTest {
    @Test
    fun `rejects a machine with fewer than 3 slots`() {
        assertFailsWith<IllegalArgumentException> {
            FruitMachineConfig(
                slotCount = 2,
                colours = listOf(BLACK, WHITE, GREEN, YELLOW),
                playCost = PLAY_COST
            )
        }
    }

    @Test
    fun `accepts a machine with 3 slots`() {
        assertDoesNotThrow {
            FruitMachineConfig(
                slotCount = 3,
                colours = listOf(BLACK, WHITE, GREEN, YELLOW),
                playCost = PLAY_COST
            )
        }
    }

    @Test
    fun `rejects a machine with a single colour`() {
        assertFailsWith<IllegalArgumentException> {
            FruitMachineConfig(
                slotCount = 4,
                colours = listOf(BLACK),
                playCost = PLAY_COST
            )
        }
    }

    @Test
    fun `accepts a machine with 2 colours`() {
        assertDoesNotThrow {
            FruitMachineConfig(
                slotCount = 4,
                colours = listOf(BLACK, WHITE),
                playCost = PLAY_COST
            )
        }
    }

    @Test
    fun `rejects duplicate colours`() {
        assertFailsWith<IllegalArgumentException> {
            FruitMachineConfig(
                slotCount = 4,
                colours = listOf(BLACK, WHITE, BLACK),
                playCost = PLAY_COST
            )
        }
    }

    @Test
    fun `rejects a play cost of zero`() {
        assertFailsWith<IllegalArgumentException> {
            FruitMachineConfig(
                slotCount = 4,
                colours = listOf(BLACK, WHITE, GREEN, YELLOW),
                playCost = euro(0)
            )
        }
    }
}

class FruitMachineTest {
    @Test
    fun `rejects a negative float`() {
        assertFailsWith<IllegalArgumentException> {
            FruitMachine(DEFAULT_CONFIG, euro(-1))
        }
    }

    @Test
    fun `rejects a float in a different currency to the play cost`() {
        assertFailsWith<IllegalArgumentException> {
            FruitMachine(DEFAULT_CONFIG, Money(1000, Currency.getInstance("USD")))
        }
    }
}
