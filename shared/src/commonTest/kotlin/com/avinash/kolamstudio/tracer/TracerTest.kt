package com.avinash.kolamstudio.tracer

import com.avinash.kolamstudio.model.Grid
import com.avinash.kolamstudio.model.MidpointState
import com.avinash.kolamstudio.model.Pattern
import kotlin.test.Test
import kotlin.test.assertEquals

class TracerTest {
    @Test
    fun allCrossGridHasGcdLoops() {
        for (rows in 1..8) {
            for (cols in 1..8) {
                val loops = trace(Pattern(Grid(rows, cols)))
                assertEquals(gcd(rows, cols), loops.size, "loops in $rows × $cols")
                // Every unit square of the box holds exactly one step of the line.
                assertEquals(4 * rows * cols, loops.sumOf { it.size }, "steps in $rows × $cols")
            }
        }
    }

    @Test
    fun mirrorsTurnTheLine() {
        // Two dots side by side with one midpoint between them.
        val grid = Grid(rows = 1, cols = 2)

        fun loopCount(state: MidpointState) = trace(Pattern(grid, listOf(state))).size

        assertEquals(1, loopCount(MidpointState.CROSS), "figure of eight")
        assertEquals(1, loopCount(MidpointState.MIRROR_H), "one loop around both dots")
        assertEquals(2, loopCount(MidpointState.MIRROR_V), "one loop around each dot")
    }

    private tailrec fun gcd(
        a: Int,
        b: Int,
    ): Int = if (b == 0) a else gcd(b, a % b)
}
