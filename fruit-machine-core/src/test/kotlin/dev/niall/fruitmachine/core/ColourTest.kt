package dev.niall.fruitmachine.core

import kotlin.test.Test
import kotlin.test.assertFailsWith

class ColourTest {
    @Test
    fun `rejects an empty name`() {
        assertFailsWith<IllegalArgumentException> { Colour("") }
    }

    @Test
    fun `rejects a whitespace-only name`() {
        assertFailsWith<IllegalArgumentException> { Colour("  ") }
    }
}
