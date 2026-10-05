package dev.niall.fruitmachine.investigations

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

// Plays lots of small machines at once, to check whether the colour representation matters
// when a service is running many machines on many threads. See README.md for how to run it
// (the thread count is set on the command line) and for the results.
//
// Each machine has its own colour list with its own String instances, as if each config had
// been deserialised from JSON separately, and every play picks a random machine. With 10,000
// machines, a machine's colours are unlikely to still be in the CPU's caches from its last play,
// unlike in ColourRepresentationBenchmark, which plays the same machine every time.
//
// Machines are only read, never updated: the float and the per-machine locking belong to the
// service, which doesn't exist yet. Each play is the same as ColourRepresentationBenchmark's
// play benchmarks: spin, evaluate, then build the colour names a response needs.
@State(Scope.Benchmark)
open class ManyMachinesBenchmark {
    // 1 machine is the cache-friendly baseline.
    @Param("1", "10000")
    var machineCount = 0

    @Param("4", "300")
    var colourCount = 0

    // The original game, as most machines are expected to be small.
    private val slotCount = 4
    private val adjacentSlotsForSmallPrize = 2

    class Machine(
        val colourNames: List<String>,
        val colours: List<ValueColour>,
        val plainColours: List<PlainColour>
    )

    private lateinit var machines: Array<Machine>

    @Setup
    open fun setUp() {
        machines = Array(machineCount) {
            // A string template builds a new String each time, so no two machines share names.
            val names = List(colourCount) { colour -> "colour-$colour" }
            Machine(names, names.map { ValueColour(it) }, names.map { PlainColour(it) })
        }
    }

    // Kotlin's Random isn't thread-safe, so each benchmark thread gets its own.
    @State(Scope.Thread)
    open class Player {
        val random = Random(nextSeed.incrementAndGet())

        companion object {
            private val nextSeed = AtomicInteger()
        }
    }

    @Benchmark
    open fun playColourList(player: Player): PlayResponse {
        val machine = machines[player.random.nextInt(machineCount)]
        val slots = List(slotCount) { machine.colours.random(player.random) }
        val outcome = computeOutcomeOfOriginal(slots, adjacentSlotsForSmallPrize)
        return PlayResponse(slots.map { it.name }, outcome)
    }

    @Benchmark
    open fun playColourListSinglePass(player: Player): PlayResponse {
        val machine = machines[player.random.nextInt(machineCount)]
        val slots = List(slotCount) { machine.colours.random(player.random) }
        val outcome = computeOutcomeOfSinglePass(slots, adjacentSlotsForSmallPrize)
        return PlayResponse(slots.map { it.name }, outcome)
    }

    @Benchmark
    open fun playPlainColourListSinglePass(player: Player): PlayResponse {
        val machine = machines[player.random.nextInt(machineCount)]
        val slots = List(slotCount) { machine.plainColours.random(player.random) }
        val outcome = computeOutcomeOfSinglePass(slots, adjacentSlotsForSmallPrize)
        return PlayResponse(slots.map { it.name }, outcome)
    }

    @Benchmark
    open fun playSpinSinglePass(player: Player): PlayResponse {
        val machine = machines[player.random.nextInt(machineCount)]
        val slots = Spin(IntArray(slotCount) { player.random.nextInt(colourCount) })
        val outcome = computeOutcomeOfSinglePass(slots, colourCount, adjacentSlotsForSmallPrize)
        return PlayResponse(List(slots.size) { machine.colourNames[slots[it].index] }, outcome)
    }
}
