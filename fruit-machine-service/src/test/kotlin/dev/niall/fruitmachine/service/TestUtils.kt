package dev.niall.fruitmachine.service

import dev.niall.fruitmachine.core.Colour
import dev.niall.fruitmachine.core.FruitMachine
import dev.niall.fruitmachine.core.FruitMachineConfig
import dev.niall.fruitmachine.core.Money
import java.util.Currency

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
