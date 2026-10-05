package dev.niall.fruitmachine.service

import dev.niall.fruitmachine.core.FruitMachine
import dev.niall.fruitmachine.core.FruitMachineEngine
import dev.niall.fruitmachine.core.PlayResult
import dev.niall.fruitmachine.service.persistence.MachineRepository
import org.springframework.stereotype.Service
import java.util.UUID

class MachineNotFoundException(id: UUID) : RuntimeException("No machine found with id $id")

@Service
class MachineService(
    private val machineRepository: MachineRepository,
    private val engine: FruitMachineEngine
) {
    fun create(machine: FruitMachine): UUID {
        val id = UUID.randomUUID()
        machineRepository.save(id, machine)
        return id
    }

    fun get(id: UUID): FruitMachine =
        machineRepository.findById(id) ?: throw MachineNotFoundException(id)

    fun play(id: UUID): PlayResult {
        // MachineRepository.update is atomic per machine, so two plays can't start from the same
        // float. If transform runs more than once, `result` ends up from the run that actually was stored.
        lateinit var result: PlayResult
        machineRepository.update(id) { machine ->
            result = engine.play(machine)
            result.machine
        } ?: throw MachineNotFoundException(id)

        return result
    }
}
