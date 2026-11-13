package com.avinash.kolamstudio.model

import kotlin.test.Test
import kotlin.test.assertEquals

class PatternTest {
    @Test
    fun withNextStateAtCyclesOneMidpointThroughAllStates() {
        val start = Pattern(Grid(rows = 2, cols = 3))
        val midpoint = start.grid.midpoints[2]

        val once = start.withNextStateAt(midpoint)
        assertEquals(MidpointState.MIRROR_H, once.stateAt(midpoint.x, midpoint.y))
        assertEquals(1, once.states.count { it != MidpointState.CROSS })

        val twice = once.withNextStateAt(midpoint)
        assertEquals(MidpointState.MIRROR_V, twice.stateAt(midpoint.x, midpoint.y))
        assertEquals(start, twice.withNextStateAt(midpoint))
    }
}
