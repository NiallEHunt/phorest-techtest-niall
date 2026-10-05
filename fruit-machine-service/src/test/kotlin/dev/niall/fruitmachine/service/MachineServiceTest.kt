package dev.niall.fruitmachine.service

import dev.niall.fruitmachine.service.persistence.InMemoryMachineRepository
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class MachineServiceTest {
    private val service = MachineService(InMemoryMachineRepository())

    @Test
    fun `a created machine can be read back`() {
        val id = service.create(DEFAULT_MACHINE)

        assertEquals(DEFAULT_MACHINE, service.get(id))
    }

    @Test
    fun `each created machine gets a new id`() {
        assertNotEquals(service.create(DEFAULT_MACHINE), service.create(DEFAULT_MACHINE))
    }

    @Test
    fun `reading an unknown machine throws MachineNotFoundException`() {
        val id = UUID.randomUUID()

        val exception = assertFailsWith<MachineNotFoundException> { service.get(id) }
        assertEquals("No machine found with id $id", exception.message)
    }
}
