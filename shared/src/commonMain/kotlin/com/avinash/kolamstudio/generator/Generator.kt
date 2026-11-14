package com.avinash.kolamstudio.generator

import com.avinash.kolamstudio.model.Grid
import com.avinash.kolamstudio.model.MidpointState
import com.avinash.kolamstudio.model.Pattern
import com.avinash.kolamstudio.tracer.trace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.random.Random

enum class Symmetry { ONE_AXIS, TWO_AXES }

private const val ATTEMPT_LIMIT = 2000

val OFFERED_SIZES = 2..8

fun offeredSymmetries(grid: Grid): List<Symmetry> =
    if (grid.rows % 2 == 0 && grid.cols % 2 == 0) listOf(Symmetry.ONE_AXIS) else Symmetry.entries

internal suspend fun generate(
    grid: Grid,
    symmetry: Symmetry,
    random: Random = Random.Default,
): Pair<Pattern?, Symmetry> =
    withContext(Dispatchers.Default) {
        val context = currentCoroutineContext()
        val pattern = search(grid, symmetry, random, context::ensureActive)
        if (pattern == null && symmetry == Symmetry.TWO_AXES) {
            search(grid, Symmetry.ONE_AXIS, random, context::ensureActive) to Symmetry.ONE_AXIS
        } else {
            pattern to symmetry
        }
    }

internal fun search(
    grid: Grid,
    symmetry: Symmetry,
    random: Random,
    checkCancellation: () -> Unit = {},
): Pattern? {
    val groups = symmetricGroups(grid, symmetry)
    var pattern = Pattern(grid)
    var loopCount = trace(pattern).size
    repeat(ATTEMPT_LIMIT) { attempt ->
        checkCancellation()
        if (loopCount == 1 && attempt >= groups.size) return pattern
        val group = groups.random(random)
        val oldState = pattern.states[group.first()]
        val newState = (MidpointState.entries - oldState).random(random)
        val candidate = pattern.copy(states = pattern.states.mapIndexed { i, state -> if (i in group) newState else state })
        val candidateLoopCount = trace(candidate).size
        if (candidateLoopCount <= loopCount) {
            pattern = candidate
            loopCount = candidateLoopCount
        }
    }
    return pattern.takeIf { loopCount == 1 }
}

private fun symmetricGroups(
    grid: Grid,
    symmetry: Symmetry,
): List<List<Int>> =
    grid.midpoints
        .groupBy { (x, y) ->
            val foldedY = if (symmetry == Symmetry.TWO_AXES) min(y, grid.height - y) else y
            min(x, grid.width - x) to foldedY
        }.values
        .map { group -> group.map { grid.midpointIndex(x = it.x, y = it.y) } }
