package dev.niall.fruitmachine.service

import dev.niall.fruitmachine.core.FruitMachine
import dev.niall.fruitmachine.core.FruitMachineEngine
import dev.niall.fruitmachine.core.PlayResult
import dev.niall.fruitmachine.service.persistence.MachineRepository
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

class MachineNotFoundException(id: UUID) : RuntimeException("No machine found with id $id")

private val logger = LoggerFactory.getLogger(MachineService::class.java)

@Service
class MachineService(
    private val machineRepository: MachineRepository,
    private val engine: FruitMachineEngine,
    private val meterRegistry: MeterRegistry
) {
    private val freePlaysCredited = Counter.builder("fruitmachine.free.plays.credited").register(meterRegistry)

    fun create(machine: FruitMachine): UUID {
        val id = UUID.randomUUID()
        machineRepository.save(id, machine)

        meterRegistry.counter("fruitmachine.machines.created").increment()
        logger.info(
            "Created machine {} with {} slots, {} colours and a play cost of {} {}",
            id, machine.config.slotCount, machine.config.colours.size,
            machine.config.playCost.minorUnits, machine.config.playCost.currency
        )
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

        record(id, result)
        return result
    }

    // Plays and payouts are tagged by outcome and currency
    private fun record(id: UUID, result: PlayResult) {
        val payout = result.payout
        meterRegistry.counter("fruitmachine.plays", "outcome", result.outcome.name).increment()
        DistributionSummary.builder("fruitmachine.payouts")
            .baseUnit("minor units")
            .tag("currency", payout.amount.currency.currencyCode)
            .register(meterRegistry)
            .record(payout.amount.minorUnits.toDouble())
        freePlaysCredited.increment(payout.freePlays.toDouble())

        logger.info(
            "Played machine {}: {}, paid out {} {} and credited {} free plays, float is now {} {}",
            id, result.outcome, payout.amount.minorUnits, payout.amount.currency, payout.freePlays,
            result.machine.float.minorUnits, result.machine.float.currency
        )
    }
}
