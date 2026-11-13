package com.avinash.kolamstudio.model

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
