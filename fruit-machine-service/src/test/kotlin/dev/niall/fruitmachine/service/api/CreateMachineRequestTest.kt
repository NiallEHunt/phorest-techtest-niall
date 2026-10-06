package dev.niall.fruitmachine.service.api

import jakarta.validation.Validation
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CreateMachineRequestTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    private val validRequest = CreateMachineRequest(
        slotCount = 4,
        colours = listOf("black", "white", "green", "yellow"),
        adjacentSlotsForSmallPrize = 2,
        playCost = MoneyDto(100, "EUR"),
        startingFloat = MoneyDto(5000, "EUR")
    )

    // The paths of the fields that failed validation, e.g. "playCost.minorUnits".
    private fun violationsOf(request: CreateMachineRequest): Set<String> =
        validator.validate(request).map { it.propertyPath.toString() }.toSet()

    @Test
    fun `a valid request has no violations`() {
        assertEquals(emptySet(), violationsOf(validRequest))
    }

    @Test
    fun `accepts the maximum slot count`() {
        assertEquals(emptySet(), violationsOf(validRequest.copy(slotCount = MAX_SLOT_COUNT)))
    }

    @Test
    fun `rejects a slot count above the maximum`() {
        assertEquals(setOf("slotCount"), violationsOf(validRequest.copy(slotCount = MAX_SLOT_COUNT + 1)))
    }

    @Test
    fun `accepts the maximum number of colours`() {
        assertEquals(emptySet(), violationsOf(validRequest.copy(colours = List(MAX_COLOURS) { "colour$it" })))
    }

    @Test
    fun `rejects more colours than the maximum`() {
        assertEquals(setOf("colours"), violationsOf(validRequest.copy(colours = List(MAX_COLOURS + 1) { "colour$it" })))
    }

    @Test
    fun `accepts the maximum play cost and starting float`() {
        val request = validRequest.copy(
            playCost = MoneyDto(MAX_AMOUNT_MINOR_UNITS, "EUR"),
            startingFloat = MoneyDto(MAX_AMOUNT_MINOR_UNITS, "EUR")
        )
        assertEquals(emptySet(), violationsOf(request))
    }

    @Test
    fun `rejects a play cost above the maximum`() {
        assertEquals(
            setOf("playCost.minorUnits"),
            violationsOf(validRequest.copy(playCost = MoneyDto(MAX_AMOUNT_MINOR_UNITS + 1, "EUR")))
        )
    }

    @Test
    fun `rejects a starting float above the maximum`() {
        assertEquals(
            setOf("startingFloat.minorUnits"),
            violationsOf(validRequest.copy(startingFloat = MoneyDto(MAX_AMOUNT_MINOR_UNITS + 1, "EUR")))
        )
    }

    @Test
    fun `reports every invalid field at once`() {
        val request = validRequest.copy(slotCount = MAX_SLOT_COUNT + 1, playCost = MoneyDto(MAX_AMOUNT_MINOR_UNITS + 1, "EUR"))
        assertEquals(setOf("slotCount", "playCost.minorUnits"), violationsOf(request))
    }

    @Test
    fun `accepts a currency with no decimal places`() {
        assertDoesNotThrow {
            validRequest.copy(playCost = MoneyDto(100, "JPY"), startingFloat = MoneyDto(5000, "JPY")).toFruitMachine()
        }
    }

    @Test
    fun `rejects a blank colour name`() {
        assertFailsWith<InvalidMachineConfigException> {
            validRequest.copy(colours = listOf("black", " ")).toFruitMachine()
        }
    }
}
