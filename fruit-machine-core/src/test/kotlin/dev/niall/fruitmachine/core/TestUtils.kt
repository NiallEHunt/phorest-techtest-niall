package dev.niall.fruitmachine.core

import java.util.Currency

internal val EUR: Currency = Currency.getInstance("EUR")
internal fun euro(minorUnits: Long) = Money(minorUnits, EUR)
internal val PLAY_COST = euro(100)
