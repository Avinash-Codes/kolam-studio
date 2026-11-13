package com.avinash.kolamstudio.model

import com.avinash.kolamstudio.generator.OFFERED_SIZES
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PatternCodeTest {
    @Test
    fun patternToCodeAndBackIsUnchanged() {
        val random = Random(seed = 7)
        for (rows in OFFERED_SIZES) {
            for (cols in OFFERED_SIZES) {
                val grid = Grid(rows, cols)
                val pattern = Pattern(grid, List(grid.midpointCount) { MidpointState.entries.random(random) })
                assertEquals(pattern, patternFromCode(pattern.toCode()), "$rows × $cols")
            }
        }
    }

    @Test
    fun codeIsShortAndUrlSafe() {
        val code = Pattern(Grid(rows = 8, cols = 8)).toCode()
        assertEquals(40, code.length)
        assertEquals(emptyList(), code.filterNot { it.isLetterOrDigit() || it in "-_" }.toList())
    }

    @Test
    fun brokenCodesAreRejected() {
        val code = Pattern(Grid(rows = 3, cols = 4)).toCode()
        assertNull(patternFromCode(""), "empty")
        assertNull(patternFromCode("not a code!"), "not Base64")
        assertNull(patternFromCode(code.dropLast(2)), "too short for its grid")
        assertNull(patternFromCode(code + "AA"), "too long for its grid")
        assertNull(patternFromCode("AAA"), "zero-sized grid")
        assertNull(patternFromCode("AQI_"), "state value 3 does not exist")
    }
}
