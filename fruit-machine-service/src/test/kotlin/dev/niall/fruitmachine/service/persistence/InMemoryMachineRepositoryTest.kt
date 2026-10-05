package dev.niall.fruitmachine.service.persistence

import dev.niall.fruitmachine.service.DEFAULT_MACHINE
import dev.niall.fruitmachine.service.euro
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InMemoryMachineRepositoryTest {
    private val repository = InMemoryMachineRepository()

    @Test
    fun `finds a saved machine by its id`() {
        val id = UUID.randomUUID()
        repository.save(id, DEFAULT_MACHINE)

        assertEquals(DEFAULT_MACHINE, repository.findById(id))
    }

    @Test
    fun `returns null for an unknown id`() {
        assertNull(repository.findById(UUID.randomUUID()))
    }

    @Test
    fun `saving with an existing id replaces the machine`() {
        val id = UUID.randomUUID()
        val updated = DEFAULT_MACHINE.copy(float = euro(1234))
        repository.save(id, DEFAULT_MACHINE)
        repository.save(id, updated)

        assertEquals(updated, repository.findById(id))
    }
}
