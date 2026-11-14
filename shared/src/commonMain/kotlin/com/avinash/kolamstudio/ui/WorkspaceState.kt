package com.avinash.kolamstudio.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.avinash.kolamstudio.generator.Symmetry
import com.avinash.kolamstudio.generator.offeredSymmetries
import com.avinash.kolamstudio.model.Grid
import com.avinash.kolamstudio.model.Pattern
import com.avinash.kolamstudio.model.Point
import com.avinash.kolamstudio.tracer.TracePath
import com.avinash.kolamstudio.tracer.trace

private const val DEFAULT_SIZE = 5

enum class Mode(
    val label: String,
) {
    EXPLORE("Explore"),
    EDIT("Edit"),
    TRACE("Trace"),
}

@Stable
class WorkspaceState(
    initialPattern: Pattern?,
) {
    var pattern by mutableStateOf(initialPattern)
        private set
    var rows by mutableIntStateOf(initialPattern?.grid?.rows ?: DEFAULT_SIZE)
        private set
    var cols by mutableIntStateOf(initialPattern?.grid?.cols ?: DEFAULT_SIZE)
        private set
    var generation by mutableIntStateOf(0)
        private set
    var traced by mutableFloatStateOf(0f)
        private set
    var mode by mutableStateOf(Mode.EXPLORE)
    var showGraph by mutableStateOf(false)
    var status by mutableStateOf("")
    private var wantedSymmetry by mutableStateOf(Symmetry.TWO_AXES)

    val grid: Grid get() = Grid(rows, cols)
    val symmetries: List<Symmetry> get() = offeredSymmetries(grid)
    val symmetry: Symmetry get() = if (wantedSymmetry in symmetries) wantedSymmetry else symmetries.first()
    val loops: List<List<Point>> by derivedStateOf { pattern?.let(::trace).orEmpty() }
    val tracePath: TracePath? by derivedStateOf { loops.singleOrNull()?.let(::TracePath) }
    val tracedFraction: Float get() = tracePath?.let { traced / it.length } ?: 0f

    fun show(pattern: Pattern) {
        this.pattern = pattern
        rows = pattern.grid.rows
        cols = pattern.grid.cols
        traced = 0f
        status = ""
    }

    fun resize(
        rows: Int,
        cols: Int,
    ) {
        if (rows == this.rows && cols == this.cols) return
        this.rows = rows
        this.cols = cols
        generation++
    }

    fun chooseSymmetry(symmetry: Symmetry) {
        wantedSymmetry = symmetry
        generation++
    }

    fun newPattern() {
        generation++
    }

    fun tapMidpoint(midpoint: Point) {
        pattern?.let { show(it.withNextStateAt(midpoint)) }
    }

    fun traceTo(
        x: Float,
        y: Float,
    ) {
        tracePath?.let { traced = it.advance(traced, x, y) }
    }

    fun restartTrace() {
        traced = 0f
    }
}
