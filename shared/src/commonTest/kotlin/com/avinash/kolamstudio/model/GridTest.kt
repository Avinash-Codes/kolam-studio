package com.avinash.kolamstudio.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GridTest {
    @Test
    fun midpointIndexMatchesPositionInMidpoints() {
        for (rows in 1..5) {
            for (cols in 1..5) {
                val grid = Grid(rows, cols)
                assertEquals(grid.midpointCount, grid.midpoints.size, "$rows × $cols")
                grid.midpoints.forEachIndexed { index, point ->
                    assertEquals(index, grid.midpointIndex(point.x, point.y), "$point in $rows × $cols")
                }
            }
        }
    }

    @Test
    fun midpointAtFindsTheMidpointUnderATap() {
        val grid = Grid(rows = 3, cols = 4)
        for (midpoint in grid.midpoints) {
            assertEquals(midpoint, grid.midpointAt(midpoint.x + 0.4f, midpoint.y - 0.4f), "near $midpoint")
        }
        assertNull(grid.midpointAt(0.2f, 1f), "box edge")
        assertNull(grid.midpointAt(-3f, 2f), "outside the box")
    }
}
