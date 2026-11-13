package com.avinash.kolamstudio.model

/** A [grid] plus one state per midpoint, in the order of [Grid.midpoints]. All `CROSS` by default. */
data class Pattern(
    val grid: Grid,
    val states: List<MidpointState> = List(grid.midpointCount) { MidpointState.CROSS },
) {
    init {
        require(states.size == grid.midpointCount) {
            "Expected ${grid.midpointCount} midpoint states, got ${states.size}"
        }
    }

    fun stateAt(
        x: Int,
        y: Int,
    ): MidpointState = states[grid.midpointIndex(x, y)]
}
