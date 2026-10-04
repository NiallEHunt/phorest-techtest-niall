package dev.niall.fruitmachine.core

import java.util.Currency

data class Money(
    val minorUnits: Long,
    val currency: Currency
) : Comparable<Money> {
    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(Math.addExact(minorUnits, other.minorUnits), currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return Money(Math.subtractExact(minorUnits, other.minorUnits), currency)
    }

    operator fun times(factor: Int): Money =
        Money(Math.multiplyExact(minorUnits, factor.toLong()), currency)

    // Rounds down, as we can't represent fractions of a minor unit.
    operator fun div(divisor: Int): Money {
        require(divisor != 0) { "Cannot divide by zero" }
        return Money(minorUnits.floorDiv(divisor), currency)
    }

    // How many whole multiples of `other` fit into this amount, rounding down.
    operator fun div(other: Money): Long {
        requireSameCurrency(other)
        require(other.minorUnits != 0L) { "Cannot divide by zero" }
        return minorUnits.floorDiv(other.minorUnits)
    }

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return minorUnits.compareTo(other.minorUnits)
    }

    private fun requireSameCurrency(other: Money) =
        require(currency == other.currency) { "Cannot combine $currency with ${other.currency}" }
}
