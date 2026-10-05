package dev.niall.fruitmachine.service.persistence

import dev.niall.fruitmachine.core.FruitMachine
import java.util.UUID

interface MachineRepository {
    fun save(id: UUID, machine: FruitMachine)
    fun findById(id: UUID): FruitMachine?

    /**
     * Replaces the machine stored under [id] with the result of [transform], atomically for that id.
     *
     * Implementations must guarantee:
     * - Updates to the same id never overwrite each other: [transform] always receives the
     *   latest stored machine.
     * - If [transform] throws, the stored machine is left unchanged and the exception propagates.
     * - [transform] runs on the caller's thread before `update` returns. It may run more than once
     *   (e.g. optimistic-locking retries); only the result of the final run is stored.
     *
     * Every implementation should pass MachineRepositoryContractTest.
     *
     * @return the stored machine, or null if no machine exists for [id] (nothing is stored).
     */
    fun update(id: UUID, transform: (FruitMachine) -> FruitMachine): FruitMachine?
}
