package dev.niall.fruitmachine.investigations

// Alternative colour types to the core's `@JvmInline value class Colour(val name: String)`,
// written only so they can be benchmarked against it.

// A plain (non-value) class. Unlike a value class, putting one in a List doesn't create a new
// wrapper: every slot points at one of the instances in the config's colour list, so a spin
// allocates nothing per slot. Equality short-circuits on identity, because data class equals
// checks `this === other` first.
data class PlainColour(val name: String)

// An Int value class: a colour is its index in the config's colour list. On its own it's
// boxed in a List just like the core's Colour, so it's only stored in a Spin.
@JvmInline
value class ColourIndex(val index: Int)

// A spin backed by a primitive IntArray, so no slot is ever boxed, but callers still see
// typed ColourIndex values rather than raw Ints.
class Spin(private val indices: IntArray) {
    val size: Int get() = indices.size

    operator fun get(slot: Int): ColourIndex = ColourIndex(indices[slot])
}
