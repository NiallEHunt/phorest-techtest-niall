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

    // computeIfPresent locks the entry for this id while `transform` runs, so updates
    // to the same machine happen one at a time while other machines are unaffected.
    override fun update(id: UUID, transform: (FruitMachine) -> FruitMachine): FruitMachine? =
        machines.computeIfPresent(id) { _, machine -> transform(machine) }
}
