package dev.niall.fruitmachine.core

import java.util.Currency

internal val EUR: Currency = Currency.getInstance("EUR")
internal fun euro(minorUnits: Long) = Money(minorUnits, EUR)
internal val PLAY_COST = euro(100)

internal val BLACK = Colour("black")
internal val GREEN = Colour("green")
internal val WHITE = Colour("white")
internal val YELLOW = Colour("yellow")

internal val DEFAULT_CONFIG = FruitMachineConfig(
    slotCount = 4,
    colours = listOf(BLACK, GREEN, WHITE, YELLOW),
    playCost = PLAY_COST
)
