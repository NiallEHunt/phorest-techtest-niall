package dev.niall.fruitmachine.service.api

import dev.niall.fruitmachine.core.Colour
import dev.niall.fruitmachine.core.FruitMachine
import dev.niall.fruitmachine.core.FruitMachineConfig
import dev.niall.fruitmachine.core.Money
import dev.niall.fruitmachine.core.Outcome
import dev.niall.fruitmachine.core.PlayResult
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Size
import java.util.Currency
import java.util.UUID

data class MoneyDto(
    @field:Max(MAX_AMOUNT_MINOR_UNITS)
    val minorUnits: Long,
    val currency: String
) {
    fun toMoney(): Money = Money(minorUnits, Currency.getInstance(currency))

    companion object {
        fun from(money: Money) = MoneyDto(money.minorUnits, money.currency.currencyCode)
    }
}

class InvalidMachineConfigException(message: String?, cause: Throwable) : RuntimeException(message, cause)

// Service limits, not game rules: keep each play cheap and amounts well clear of overflow.
const val MAX_SLOT_COUNT = 10_000
const val MAX_COLOURS = 1_000
const val MAX_AMOUNT_MINOR_UNITS = 1_000_000_000_000L

data class CreateMachineRequest(
    @field:Max(MAX_SLOT_COUNT.toLong())
    val slotCount: Int,
    @field:Size(max = MAX_COLOURS)
    val colours: List<String>,
    val adjacentSlotsForSmallPrize: Int,
    @field:Valid
    val playCost: MoneyDto,
    @field:Valid
    val startingFloat: MoneyDto
) {
    fun toFruitMachine(): FruitMachine = try {
        val config = FruitMachineConfig(
            slotCount = slotCount,
            colours = colours.map(::Colour),
            adjacentSlotsForSmallPrize = adjacentSlotsForSmallPrize,
            playCost = playCost.toMoney()
        )
        FruitMachine(config, startingFloat.toMoney())
    } catch (e: IllegalArgumentException) {
        throw InvalidMachineConfigException(e.message, e)
    }
}

data class MachineResponse(
    val id: UUID,
    val slotCount: Int,
    val colours: List<String>,
    val adjacentSlotsForSmallPrize: Int,
    val playCost: MoneyDto,
    val float: MoneyDto
) {
    companion object {
        fun from(id: UUID, machine: FruitMachine) = MachineResponse(
            id = id,
            slotCount = machine.config.slotCount,
            colours = machine.config.colours.map { it.name },
            adjacentSlotsForSmallPrize = machine.config.adjacentSlotsForSmallPrize,
            playCost = MoneyDto.from(machine.config.playCost),
            float = MoneyDto.from(machine.float)
        )
    }
}

data class PlayResponse(
    val slots: List<String>,
    val outcome: Outcome,
    val payout: MoneyDto,
    val freePlaysCredited: Long,
    val float: MoneyDto
) {
    companion object {
        fun from(result: PlayResult) = PlayResponse(
            slots = result.slots.map { it.name },
            outcome = result.outcome,
            payout = MoneyDto.from(result.payout.amount),
            freePlaysCredited = result.payout.freePlays,
            float = MoneyDto.from(result.machine.float)
        )
    }
}
