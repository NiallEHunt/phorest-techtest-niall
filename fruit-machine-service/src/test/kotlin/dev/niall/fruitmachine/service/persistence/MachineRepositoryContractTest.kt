package dev.niall.fruitmachine.service.persistence

import dev.niall.fruitmachine.service.DEFAULT_MACHINE
import dev.niall.fruitmachine.service.euro
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

// The behaviour every MachineRepository must have. Each implementation's test extends this
// and provides a fresh repository, so a new implementation can't pass without meeting the contract.
abstract class MachineRepositoryContractTest {
    protected abstract fun createRepository(): MachineRepository

    private val repository by lazy { createRepository() }

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

    @Test
    fun `update replaces the machine and returns the result`() {
        val id = UUID.randomUUID()
        repository.save(id, DEFAULT_MACHINE)

        val updated = repository.update(id) { it.copy(float = euro(1234)) }

        assertEquals(euro(1234), updated?.float)
        assertEquals(updated, repository.findById(id))
    }

    @Test
    fun `update returns null and stores nothing for an unknown id`() {
        val id = UUID.randomUUID()

        assertNull(repository.update(id) { DEFAULT_MACHINE })
        assertNull(repository.findById(id))
    }

    @Test
    fun `a failing update leaves the machine unchanged`() {
        val id = UUID.randomUUID()
        repository.save(id, DEFAULT_MACHINE)

        assertFailsWith<IllegalStateException> { repository.update(id) { error("boom") } }
        assertEquals(DEFAULT_MACHINE, repository.findById(id))
    }

    @Test
    fun `concurrent updates to the same machine are never lost`() {
        val id = UUID.randomUUID()
        repository.save(id, DEFAULT_MACHINE)
        val updates = 8_000

        val executor = Executors.newFixedThreadPool(8)
        repeat(updates) {
            executor.submit { repository.update(id) { it.copy(float = it.float + euro(1)) } }
        }
        executor.shutdown()
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS))

        // Each update adds exactly one cent, so any lost update shows up as a missing cent.
        assertEquals(DEFAULT_MACHINE.float + euro(updates.toLong()), repository.findById(id)?.float)
    }
}
