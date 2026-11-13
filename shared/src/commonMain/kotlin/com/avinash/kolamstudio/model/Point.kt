package com.avinash.kolamstudio.model

/** A lattice point in grid units. Dots sit at odd coordinates; the line visits points where x + y is odd. */
data class Point(
    val x: Int,
    val y: Int,
)
