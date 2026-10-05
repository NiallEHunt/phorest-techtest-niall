package dev.niall.fruitmachine.service.persistence

import dev.niall.fruitmachine.core.FruitMachine
import org.springframework.stereotype.Repository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Repository
class InMemoryMachineRepository : MachineRepository {
    // Concurrent because Spring serves requests on multiple threads.
    private val machines = ConcurrentHashMap<UUID, FruitMachine>()

    override fun save(id: UUID, machine: FruitMachine) {
        machines[id] = machine
    }

    override fun findById(id: UUID): FruitMachine? = machines[id]
}
