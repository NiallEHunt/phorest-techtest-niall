package dev.niall.fruitmachine.service

import dev.niall.fruitmachine.core.FruitMachine
import dev.niall.fruitmachine.service.persistence.MachineRepository
import org.springframework.stereotype.Service
import java.util.UUID

class MachineNotFoundException(id: UUID) : RuntimeException("No machine found with id $id")

@Service
class MachineService(private val machineRepository: MachineRepository) {
    fun create(machine: FruitMachine): UUID {
        val id = UUID.randomUUID()
        machineRepository.save(id, machine)
        return id
    }

    fun get(id: UUID): FruitMachine =
        machineRepository.findById(id) ?: throw MachineNotFoundException(id)
}
