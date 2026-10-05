package dev.niall.fruitmachine.investigations

import dev.niall.fruitmachine.core.Outcome
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlin.random.Random

// Compares the core's original String-backed Colour value class (ValueColour, see OriginalCore.kt)
// against alternative representations, for spinning, evaluating and a whole play. See README.md
// for how to run it and for the results.
//
// The class and its methods are `open` because JMH generates subclasses of benchmark
// classes, and Kotlin classes are final by default.
@State(Scope.Benchmark)
open class ColourRepresentationBenchmark {
    @Param("4", "100", "10000", "1000000")
    var slotCount = 0

    // 300 is above 127, the largest Integer the JVM caches, so most List<Int> slots need a
    // newly allocated Integer. With 100,000 colours a large row has mostly distinct colours,
    // so the distinct-colour set keeps growing instead of stopping at a few hundred.
    @Param("4", "300", "100000")
    var colourCount = 0

    private val adjacentSlotsForSmallPrize = 2

    private lateinit var colours: List<ValueColour>
    private lateinit var plainColours: List<PlainColour>
    private lateinit var colourNames: List<String>
    private lateinit var random: Random

    // The same spin in each representation, for the evaluate benchmarks.
    private lateinit var colourSlots: List<ValueColour>
    private lateinit var plainColourSlots: List<PlainColour>
    private lateinit var intListSlots: List<Int>
    private lateinit var intArraySlots: IntArray
    private lateinit var spinSlots: Spin

    @Setup
    open fun setUp() {
        colourNames = List(colourCount) { "colour-$it" }
        colours = colourNames.map { ValueColour(it) }
        plainColours = colourNames.map { PlainColour(it) }
        random = Random(42)

        val indices = IntArray(slotCount) { random.nextInt(colourCount) }
        colourSlots = indices.map { colours[it] }
        plainColourSlots = indices.map { plainColours[it] }
        intListSlots = indices.toList()
        intArraySlots = indices
        spinSlots = Spin(indices)

        checkEvaluatorsAgree()
    }

    // Spinning: the same as FruitMachineEngine.spin, in each representation.

    @Benchmark
    open fun spinColourList(): List<ValueColour> = List(slotCount) { colours.random(random) }

    @Benchmark
    open fun spinPlainColourList(): List<PlainColour> = List(slotCount) { plainColours.random(random) }

    @Benchmark
    open fun spinIntList(): List<Int> = List(slotCount) { random.nextInt(colourCount) }

    @Benchmark
    open fun spinIntArray(): IntArray = IntArray(slotCount) { random.nextInt(colourCount) }

    // Evaluating with the core's original algorithm (see OriginalCore.kt), against Int copies of it.

    @Benchmark
    open fun evaluateColourList(): Outcome = computeOutcomeOfOriginal(colourSlots, adjacentSlotsForSmallPrize)

    @Benchmark
    open fun evaluateIntList(): Outcome = computeOutcomeOfIntList(intListSlots, adjacentSlotsForSmallPrize)

    @Benchmark
    open fun evaluateIntArray(): Outcome = computeOutcomeOfIntArray(intArraySlots, adjacentSlotsForSmallPrize)

    @Benchmark
    open fun evaluateIntArrayPrimitive(): Outcome =
        computeOutcomeOfIntArrayPrimitive(intArraySlots, colourCount, adjacentSlotsForSmallPrize)

    // Evaluating with the single-pass algorithm, which stops early.

    @Benchmark
    open fun evaluateColourListSinglePass(): Outcome =
        computeOutcomeOfSinglePass(colourSlots, adjacentSlotsForSmallPrize)

    @Benchmark
    open fun evaluatePlainColourListSinglePass(): Outcome =
        computeOutcomeOfSinglePass(plainColourSlots, adjacentSlotsForSmallPrize)

    @Benchmark
    open fun evaluateSpinSinglePass(): Outcome =
        computeOutcomeOfSinglePass(spinSlots, colourCount, adjacentSlotsForSmallPrize)

    // A whole play: spin, evaluate, then turn the slots into the colour names a response needs.
    // This charges the index-based Spin for looking its names back up, which the separate
    // spin and evaluate benchmarks don't.

    @Benchmark
    open fun playColourList(): PlayResponse {
        val slots = List(slotCount) { colours.random(random) }
        val outcome = computeOutcomeOfOriginal(slots, adjacentSlotsForSmallPrize)
        return PlayResponse(slots.map { it.name }, outcome)
    }

    @Benchmark
    open fun playColourListSinglePass(): PlayResponse {
        val slots = List(slotCount) { colours.random(random) }
        val outcome = computeOutcomeOfSinglePass(slots, adjacentSlotsForSmallPrize)
        return PlayResponse(slots.map { it.name }, outcome)
    }

    @Benchmark
    open fun playPlainColourListSinglePass(): PlayResponse {
        val slots = List(slotCount) { plainColours.random(random) }
        val outcome = computeOutcomeOfSinglePass(slots, adjacentSlotsForSmallPrize)
        return PlayResponse(slots.map { it.name }, outcome)
    }

    @Benchmark
    open fun playSpinSinglePass(): PlayResponse {
        val slots = Spin(IntArray(slotCount) { random.nextInt(colourCount) })
        val outcome = computeOutcomeOfSinglePass(slots, colourCount, adjacentSlotsForSmallPrize)
        return PlayResponse(List(slots.size) { colourNames[slots[it].index] }, outcome)
    }

    // A faster evaluator is only useful if it gives the same answers as the core, so this checks
    // every alternative against the core before anything is timed: on the benchmark's own spin,
    // and on many small random spins, which reach all four outcomes.
    private fun checkEvaluatorsAgree() {
        checkAgree(colourSlots.map { it.name }, colourCount)

        val checkRandom = Random(7)
        repeat(10_000) {
            val checkColourCount = checkRandom.nextInt(2, 6)
            val names = List(checkRandom.nextInt(3, 8)) { "colour-${checkRandom.nextInt(checkColourCount)}" }
            checkAgree(names, checkColourCount)
        }
    }

    private fun checkAgree(names: List<String>, colourCount: Int) {
        val indices = names.map { it.removePrefix("colour-").toInt() }.toIntArray()
        val expected = computeOutcomeOfOriginal(names.map { ValueColour(it) }, adjacentSlotsForSmallPrize)
        val actual = listOf(
            computeOutcomeOfIntList(indices.toList(), adjacentSlotsForSmallPrize),
            computeOutcomeOfIntArray(indices, adjacentSlotsForSmallPrize),
            computeOutcomeOfIntArrayPrimitive(indices, colourCount, adjacentSlotsForSmallPrize),
            computeOutcomeOfSinglePass(names.map { ValueColour(it) }, adjacentSlotsForSmallPrize),
            computeOutcomeOfSinglePass(names.map { PlainColour(it) }, adjacentSlotsForSmallPrize),
            computeOutcomeOfSinglePass(Spin(indices), colourCount, adjacentSlotsForSmallPrize),
        )
        check(actual.all { it == expected }) { "Evaluators disagree on $names: core gave $expected, others gave $actual" }
    }
}

data class PlayResponse(val slots: List<String>, val outcome: Outcome)
