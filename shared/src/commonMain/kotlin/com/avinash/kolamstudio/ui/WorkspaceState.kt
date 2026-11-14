package com.avinash.kolamstudio.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.avinash.kolamstudio.generator.Symmetry
import com.avinash.kolamstudio.generator.offeredSymmetries
import com.avinash.kolamstudio.model.Grid
import com.avinash.kolamstudio.model.Pattern
import com.avinash.kolamstudio.model.Point
import com.avinash.kolamstudio.model.toCode
import com.avinash.kolamstudio.tracer.TracePath
import com.avinash.kolamstudio.tracer.trace

internal enum class Mode(
    val label: String,
) {
    EXPLORE("Explore"),
    EDIT("Edit"),
    TRACE("Trace"),
}

internal class GenerationRequest(
    val grid: Grid,
    val symmetry: Symmetry,
)

internal data class PatternEdit(
    val pattern: Pattern,
    val midpoint: Point,
    val previousLoops: List<List<Point>>,
)

@Stable
internal class WorkspaceState(
    initialPattern: Pattern?,
    initialLesson: TopologyLesson = TopologyLesson.JOIN,
) {
    var lesson by mutableStateOf(if (initialPattern == null) initialLesson else null)
        private set
    var pattern by mutableStateOf(initialPattern ?: initialLesson.pattern())
        private set
    var grid by mutableStateOf(pattern.grid)
        private set
    var request by mutableStateOf<GenerationRequest?>(null)
        private set
    private var traced by mutableFloatStateOf(0f)
    var mode by mutableStateOf(if (initialPattern == null) Mode.EDIT else Mode.EXPLORE)
        private set
    var showGraph by mutableStateOf(initialPattern == null)
    var status by mutableStateOf("")
    private var wantedSymmetry by mutableStateOf(Symmetry.TWO_AXES)
    private val edits = mutableStateListOf<PatternEdit>()

    val lastEdit: PatternEdit? get() = edits.lastOrNull()
    val symmetries: List<Symmetry> get() = offeredSymmetries(grid)
    val symmetry: Symmetry get() = if (wantedSymmetry in symmetries) wantedSymmetry else symmetries.first()
    val loops: List<List<Point>> by derivedStateOf { trace(pattern) }
    val tracePath: TracePath? by derivedStateOf { loops.singleOrNull()?.let(::TracePath) }
    val tracedFraction: Float get() = tracePath?.let { traced / it.length } ?: 0f
    val lessonComplete: Boolean
        get() =
            lesson?.let { activeLesson ->
                loops.size == activeLesson.goalLoops &&
                    edits.firstOrNull()?.pattern != pattern && edits.any { it.midpoint == activeLesson.midpoint }
            } ?: false
    val highlightedMidpoint: Point?
        get() = lesson?.midpoint?.takeUnless { target -> edits.any { it.midpoint == target } } ?: lastEdit?.midpoint
    val affectedLoops: Set<Int> by derivedStateOf {
        val previous = lastEdit?.previousLoops ?: return@derivedStateOf emptySet()
        loops.indices.filterTo(mutableSetOf()) { index -> loops[index] !in previous }
    }
    val urlFragment: String
        get() = lesson?.takeIf { edits.isEmpty() }?.fragment ?: pattern.toCode()

    fun openLesson(lesson: TopologyLesson) {
        openPattern(lesson.pattern())
        this.lesson = lesson
        mode = Mode.EDIT
        showGraph = true
    }

    fun openPattern(pattern: Pattern) {
        request = null
        edits.clear()
        lesson = null
        grid = pattern.grid
        show(pattern)
        if (mode == Mode.TRACE && tracePath == null) mode = Mode.EDIT
    }

    fun selectMode(mode: Mode) {
        if (mode != Mode.TRACE || tracePath != null) this.mode = mode
    }

    fun resize(grid: Grid) {
        if (grid == this.grid) return
        this.grid = grid
        newPattern()
    }

    fun chooseSymmetry(symmetry: Symmetry) {
        wantedSymmetry = symmetry
        newPattern()
    }

    fun newPattern() {
        request = GenerationRequest(grid, symmetry)
        status = "Generating…"
    }

    fun finishGeneration(
        generation: GenerationRequest,
        result: Pattern?,
        actualSymmetry: Symmetry,
    ) {
        if (request !== generation) return
        if (result == null) {
            request = null
            grid = pattern.grid
            status = "Couldn't find a single loop. Try again."
        } else {
            openPattern(result)
            if (actualSymmetry != generation.symmetry) {
                wantedSymmetry = actualSymmetry
                status = "Used one mirror to keep the pattern in one loop."
            }
        }
    }

    fun tapMidpoint(midpoint: Point) {
        if (mode != Mode.EDIT || midpoint !in pattern.grid.midpoints) return
        request = null
        grid = pattern.grid
        edits.add(PatternEdit(pattern, midpoint, loops))
        show(pattern.withNextStateAt(midpoint))
    }

    fun undo() {
        val edit = edits.removeLastOrNull() ?: return
        show(edit.pattern)
    }

    fun traceTo(
        x: Float,
        y: Float,
    ) {
        tracePath?.let { traced = it.advance(progress = traced, x = x, y = y) }
    }

    fun restartTrace() {
        traced = 0f
    }

    private fun show(pattern: Pattern) {
        this.pattern = pattern
        traced = 0f
        status = ""
    }
}
