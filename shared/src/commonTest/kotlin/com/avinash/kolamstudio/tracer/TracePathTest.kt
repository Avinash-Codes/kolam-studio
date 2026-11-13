package com.avinash.kolamstudio.tracer

import com.avinash.kolamstudio.model.Grid
import com.avinash.kolamstudio.model.Pattern
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TracePathTest {
    private val path = TracePath(trace(Pattern(Grid(rows = 3, cols = 4))).single())

    @Test
    fun followingThePathInOrderCompletesIt() {
        val progress = path.samples.fold(0f) { progress, sample -> path.advance(progress, sample.x, sample.y) }
        assertEquals(path.length, progress)
    }

    @Test
    fun progressDoesNotAdvanceWhenThePointerJumpsToAnotherStrandAtACrossing() {
        val samples = path.samples
        val first = samples.indices.first { i -> crossingPartner(i) != null }
        val second = crossingPartner(first)!!

        val atCrossing = samples.take(first + 1).fold(0f) { progress, sample -> path.advance(progress, sample.x, sample.y) }
        assertEquals(samples[first].distance, atCrossing)

        val alongOtherStrand = samples.subList(second + 1, second + 13)
        val afterJump = alongOtherStrand.fold(atCrossing) { progress, sample -> path.advance(progress, sample.x, sample.y) }
        assertEquals(atCrossing, afterJump)

        val next = samples[first + 1]
        assertTrue(path.advance(afterJump, next.x, next.y) > atCrossing, "the right strand still advances")
    }

    private fun crossingPartner(index: Int): Int? {
        val here = path.samples[index]
        return (index + 1 until path.samples.lastIndex).firstOrNull { other ->
            val there = path.samples[other]
            hypot(here.x - there.x, here.y - there.y) < 0.001f && there.distance - here.distance > 3f
        }
    }
}
