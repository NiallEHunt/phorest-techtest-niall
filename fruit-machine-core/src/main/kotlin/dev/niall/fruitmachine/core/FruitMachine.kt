package dev.niall.fruitmachine.core

data class FruitMachineConfig(
    val slotCount: Int,
    val colours: List<Colour>,
    val adjacentSlotsForSmallPrize: Int,
    val playCost: Money
) {
    init {
        require(slotCount > 2) { "A slot count of 2 or less will result in a machine where every spin wins a prize" }
        require(colours.size >= 2) { "Less than 2 colours in a machine will result in every spin winning a prize" }
        require(colours.distinct().size == colours.size) { "All colours must be unique" }
        require(adjacentSlotsForSmallPrize in 2..slotCount) { "The adjacent slots for a small prize must be between 2 and the slot count" }
        require(playCost > Money(0, playCost.currency)) { "The play cost must be greater than zero" }
    }
}

data class FruitMachine(
    val config: FruitMachineConfig,
    val float: Money
) {
    init {
        require(float >= Money(0, float.currency)) { "A machine cannot have a negative float" }
        require(float.currency == config.playCost.currency) { "A machine must use the same currency for its float and for its play cost" }
    }
}