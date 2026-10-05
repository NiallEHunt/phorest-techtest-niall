package dev.niall.fruitmachine.service

import dev.niall.fruitmachine.core.FruitMachine
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class MachineService {
    // TODO: this is a placeholder until we persist created machines
    fun create(machine: FruitMachine): UUID = UUID.randomUUID()
}
