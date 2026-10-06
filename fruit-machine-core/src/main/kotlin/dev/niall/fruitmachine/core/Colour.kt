package dev.niall.fruitmachine.core

data class Colour(val name: String) {
    init {
        require(name.isNotBlank()) { "A colour must have a name" }
    }
}
