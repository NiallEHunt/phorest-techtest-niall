package dev.niall.fruitmachine.service

import dev.niall.fruitmachine.core.Colour
import dev.niall.fruitmachine.core.FruitMachine
import dev.niall.fruitmachine.core.FruitMachineConfig
import dev.niall.fruitmachine.core.Money
import java.util.Currency
import kotlin.random.Random

internal val EUR: Currency = Currency.getInstance("EUR")
internal fun euro(minorUnits: Long) = Money(minorUnits, EUR)

internal val DEFAULT_MACHINE = FruitMachine(
    config = FruitMachineConfig(
        slotCount = 4,
        colours = listOf(Colour("black"), Colour("white"), Colour("green"), Colour("yellow")),
        adjacentSlotsForSmallPrize = 2,
        playCost = euro(100)
    ),
    float = euro(5000)
)

// Spins the given colour indices in order, repeating. With 4 colours, each spin draws exactly
// one value, so sequenceRandom(0) always spins the first colour (a jackpot) and
// sequenceRandom(0, 1) alternates the first two colours (no prize).
internal fun sequenceRandom(vararg colourIndices: Int) = object : Random() {
    private var next = 0
    override fun nextBits(bitCount: Int): Int = colourIndices[next++ % colourIndices.size]
}
