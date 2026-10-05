package dev.niall.fruitmachine.service.api

import dev.niall.fruitmachine.core.Colour
import dev.niall.fruitmachine.core.FruitMachine
import dev.niall.fruitmachine.core.FruitMachineConfig
import dev.niall.fruitmachine.core.Money
import java.util.Currency
import java.util.UUID

data class MoneyDto(
    val minorUnits: Long,
    val currency: String
) {
    fun toMoney(): Money = Money(minorUnits, Currency.getInstance(currency))

    companion object {
        fun from(money: Money) = MoneyDto(money.minorUnits, money.currency.currencyCode)
    }
}

data class CreateMachineRequest(
    val slotCount: Int,
    val colours: List<String>,
    val adjacentSlotsForSmallPrize: Int,
    val playCost: MoneyDto,
    val startingFloat: MoneyDto
) {
    fun toFruitMachine(): FruitMachine {
        val config = FruitMachineConfig(
            slotCount = slotCount,
            colours = colours.map(::Colour),
            adjacentSlotsForSmallPrize = adjacentSlotsForSmallPrize,
            playCost = playCost.toMoney()
        )
        return FruitMachine(config, startingFloat.toMoney())
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
