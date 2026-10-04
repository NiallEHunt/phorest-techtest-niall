package dev.niall.fruitmachine.core

import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private val USD = Currency.getInstance("USD")

class MoneyTest {
    @Test
    fun `adds two amounts`() {
        assertEquals(euro(350), euro(200) + euro(150))
    }

    @Test
    fun `subtracts two amounts`() {
        assertEquals(euro(50), euro(200) - euro(150))
    }

    @Test
    fun `multiplies by a whole number`() {
        assertEquals(euro(500), euro(100) * 5)
    }

    @Test
    fun `divides by a whole number`() {
        assertEquals(euro(50), euro(100) / 2)
    }

    @Test
    fun `division rounds down to the nearest minor unit`() {
        assertEquals(euro(50), euro(101) / 2)
    }

    @Test
    fun `rejects dividing by zero`() {
        assertFailsWith<IllegalArgumentException> { euro(100) / 0 }
    }

    @Test
    fun `dividing by an amount counts how many times it fits`() {
        assertEquals(3, euro(300) / euro(100))
    }

    @Test
    fun `dividing by an amount rounds down`() {
        assertEquals(1, euro(150) / euro(100))
    }

    @Test
    fun `rejects dividing by a zero amount`() {
        assertFailsWith<IllegalArgumentException> { euro(100) / euro(0) }
    }

    @Test
    fun `rejects dividing by a different currency`() {
        assertFailsWith<IllegalArgumentException> { euro(100) / Money(100, USD) }
    }

    @Test
    fun `compares amounts`() {
        assertTrue(euro(200) > euro(150))
        assertTrue(euro(150) < euro(200))
    }

    @Test
    fun `amounts in different currencies are not equal`() {
        assertNotEquals(euro(100), Money(100, USD))
    }

    @Test
    fun `rejects adding different currencies`() {
        assertFailsWith<IllegalArgumentException> { euro(100) + Money(100, USD) }
    }

    @Test
    fun `rejects subtracting different currencies`() {
        assertFailsWith<IllegalArgumentException> { euro(100) - Money(100, USD) }
    }

    @Test
    fun `rejects comparing different currencies`() {
        assertFailsWith<IllegalArgumentException> { euro(100) > Money(100, USD) }
    }

    @Test
    fun `rejects an addition that would overflow`() {
        assertFailsWith<ArithmeticException> { euro(Long.MAX_VALUE) + euro(1) }
    }

    @Test
    fun `rejects a multiplication that would overflow`() {
        assertFailsWith<ArithmeticException> { euro(Long.MAX_VALUE) * 2 }
    }
}
