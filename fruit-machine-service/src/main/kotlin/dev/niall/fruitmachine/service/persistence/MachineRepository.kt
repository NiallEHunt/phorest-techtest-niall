package dev.niall.fruitmachine.service.persistence

import dev.niall.fruitmachine.core.FruitMachine
import java.util.UUID

interface MachineRepository {
    fun save(id: UUID, machine: FruitMachine)
    fun findById(id: UUID): FruitMachine?
}
