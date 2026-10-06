package dev.niall.fruitmachine.service

import dev.niall.fruitmachine.core.FruitMachineEngine
import dev.niall.fruitmachine.core.Outcome
import dev.niall.fruitmachine.service.persistence.InMemoryMachineRepository
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MachineServiceTest {
    private val meterRegistry = SimpleMeterRegistry()

    private fun serviceWith(random: Random) =
        MachineService(InMemoryMachineRepository(), FruitMachineEngine(random), meterRegistry)

    private val service = serviceWith(Random(10))

    @Test
    fun `a created machine can be read back`() {
        val id = service.create(DEFAULT_MACHINE)

        assertEquals(DEFAULT_MACHINE, service.get(id))
    }

    @Test
    fun `each created machine gets a new id`() {
        assertNotEquals(service.create(DEFAULT_MACHINE), service.create(DEFAULT_MACHINE))
    }

    @Test
    fun `reading an unknown machine throws MachineNotFoundException`() {
        val id = UUID.randomUUID()

        val exception = assertFailsWith<MachineNotFoundException> { service.get(id) }
        assertEquals("No machine found with id $id", exception.message)
    }

    @Test
    fun `a play stores the machine's new float`() {
        val service = serviceWith(sequenceRandom(0))
        val id = service.create(DEFAULT_MACHINE)

        val result = service.play(id)

        // A jackpot pays out the starting float plus the stake for this play.
        assertEquals(Outcome.JACKPOT, result.outcome)
        assertEquals(euro(5100), result.payout.amount)
        assertEquals(euro(0), service.get(id).float)
    }

    @Test
    fun `a losing play keeps the stake in the float`() {
        val service = serviceWith(sequenceRandom(0, 1))
        val id = service.create(DEFAULT_MACHINE)

        val result = service.play(id)

        assertEquals(Outcome.NO_PRIZE, result.outcome)
        assertEquals(euro(5100), service.get(id).float)
    }

    @Test
    fun `playing an unknown machine throws MachineNotFoundException`() {
        assertFailsWith<MachineNotFoundException> { service.play(UUID.randomUUID()) }
    }

    @Test
    fun `creating a machine is counted`() {
        service.create(DEFAULT_MACHINE)

        assertEquals(1.0, meterRegistry.get("fruitmachine.machines.created").counter().count())
    }

    @Test
    fun `a play records its outcome and payout`() {
        val service = serviceWith(sequenceRandom(0))
        val id = service.create(DEFAULT_MACHINE)

        service.play(id)

        assertEquals(1.0, meterRegistry.get("fruitmachine.plays").tag("outcome", "JACKPOT").counter().count())
        val payouts = meterRegistry.get("fruitmachine.payouts").tag("currency", "EUR").summary()
        assertEquals(1, payouts.count())
        assertEquals(5100.0, payouts.totalAmount())
    }

    @Test
    fun `a play records the free plays it credits`() {
        // Black, black, white, green: a small prize worth €5, but the float only holds the €1 stake.
        val service = serviceWith(sequenceRandom(0, 0, 1, 2))
        val id = service.create(DEFAULT_MACHINE.copy(float = euro(0)))

        service.play(id)

        assertEquals(4.0, meterRegistry.get("fruitmachine.free.plays.credited").counter().count())
    }
}
