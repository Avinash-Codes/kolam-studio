package com.avinash.kolamstudio.generator

import com.avinash.kolamstudio.model.Grid
import com.avinash.kolamstudio.model.Pattern
import com.avinash.kolamstudio.tracer.trace
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GeneratorTest {
    @Test
    fun everyOfferedSizeAndSymmetryGeneratesOneLoop() {
        forEachOfferedPattern { pattern, _, label ->
            assertEquals(1, trace(pattern).size, label)
        }
    }

    @Test
    fun generatedPatternsAreSymmetric() {
        forEachOfferedPattern { pattern, symmetry, label ->
            val grid = pattern.grid
            for ((x, y) in grid.midpoints) {
                val state = pattern.stateAt(x, y)
                assertEquals(state, pattern.stateAt(grid.width - x, y), "$label left-right at ($x, $y)")
                if (symmetry == Symmetry.TWO_AXES) {
                    assertEquals(state, pattern.stateAt(x, grid.height - y), "$label top-bottom at ($x, $y)")
                }
            }
        }
    }

    private fun forEachOfferedPattern(check: (Pattern, Symmetry, String) -> Unit) {
        for (rows in OFFERED_SIZES) {
            for (cols in OFFERED_SIZES) {
                val grid = Grid(rows, cols)
                for (symmetry in offeredSymmetries(grid)) {
                    repeat(SEEDS) { seed ->
                        val label = "seed $seed, $rows × $cols $symmetry"
                        check(assertNotNull(search(grid, symmetry, Random(seed)), label), symmetry, label)
                    }
                }
            }
        }
    }

    private companion object {
        const val SEEDS = 10
    }
}
