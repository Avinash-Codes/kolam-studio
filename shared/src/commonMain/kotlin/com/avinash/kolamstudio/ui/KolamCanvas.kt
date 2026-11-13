package com.avinash.kolamstudio.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.toSize
import com.avinash.kolamstudio.model.Grid
import com.avinash.kolamstudio.model.MidpointState
import com.avinash.kolamstudio.model.Pattern
import com.avinash.kolamstudio.model.Point
import com.avinash.kolamstudio.tracer.trace
import kotlin.math.min

private const val DRAW_MILLIS = 2500
private const val FIRST_LOOP_HUE = 260f
private const val GOLDEN_ANGLE = 137.5f
private const val WALL_HALF_LENGTH = 0.35f

@Composable
fun KolamCanvas(
    pattern: Pattern,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    showGraph: Boolean = false,
    onMidpointTap: ((Point) -> Unit)? = null,
) {
    val grid = pattern.grid
    val loops = remember(pattern) { trace(pattern) }
    val progress = remember(pattern, animate) { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(progress) { progress.animateTo(1f, tween(DRAW_MILLIS, easing = LinearEasing)) }
    val dotColor = MaterialTheme.colorScheme.onSurface
    val graphColor = MaterialTheme.colorScheme.outline
    val tapModifier =
        if (onMidpointTap == null) {
            Modifier
        } else {
            Modifier.pointerInput(grid, onMidpointTap) {
                detectTapGestures { tap ->
                    val frame = GridFrame(grid, size.toSize())
                    val position = (tap - frame.origin) / frame.unit
                    grid.midpointAt(position.x, position.y)?.let(onMidpointTap)
                }
            }
        }
    Spacer(
        modifier.then(tapModifier).drawWithCache {
            val frame = GridFrame(grid, size)
            val unit = frame.unit
            val corners = loops.map { loop -> loop.map(frame::toOffset) }
            val curves = corners.map(::smoothPath)
            val zigzags = corners.map(::closedPolyline)
            val dots = (0 until grid.rows).flatMap { row -> (0 until grid.cols).map { col -> frame.toOffset(grid.dot(row, col)) } }
            val measure = PathMeasure()
            val curveStroke = Stroke(width = unit / 8, cap = StrokeCap.Round, join = StrokeJoin.Round)
            onDrawBehind {
                if (showGraph) {
                    zigzags.forEach { drawPath(it, graphColor, style = Stroke(width = unit / 32)) }
                    for (midpoint in grid.midpoints) {
                        drawMidpoint(pattern.stateAt(midpoint.x, midpoint.y), frame.toOffset(midpoint), unit, graphColor)
                    }
                }
                curves.forEachIndexed { index, curve ->
                    measure.setPath(curve, forceClosed = false)
                    val drawn = Path()
                    measure.getSegment(0f, measure.length * progress.value, drawn)
                    drawPath(drawn, loopColor(index), style = curveStroke)
                }
                dots.forEach { drawCircle(dotColor, radius = unit / 6, center = it) }
            }
        },
    )
}

private class GridFrame(
    grid: Grid,
    size: Size,
) {
    val unit = min(size.width / grid.width, size.height / grid.height)
    val origin = Offset((size.width - unit * grid.width) / 2, (size.height - unit * grid.height) / 2)

    fun toOffset(point: Point) = origin + Offset(point.x * unit, point.y * unit)
}

private fun loopColor(index: Int) = Color.hsv((FIRST_LOOP_HUE + index * GOLDEN_ANGLE) % 360f, 0.55f, 0.65f)

private fun DrawScope.drawMidpoint(
    state: MidpointState,
    center: Offset,
    unit: Float,
    color: Color,
) {
    val reach =
        when (state) {
            MidpointState.CROSS -> return drawCircle(color, radius = unit / 12, center = center)
            MidpointState.MIRROR_H -> Offset(WALL_HALF_LENGTH * unit, 0f)
            MidpointState.MIRROR_V -> Offset(0f, WALL_HALF_LENGTH * unit)
        }
    drawLine(color, center - reach, center + reach, strokeWidth = unit / 10, cap = StrokeCap.Round)
}

private fun closedPolyline(corners: List<Offset>): Path {
    val path = Path()
    corners.forEachIndexed { index, corner ->
        if (index == 0) path.moveTo(corner.x, corner.y) else path.lineTo(corner.x, corner.y)
    }
    path.close()
    return path
}

private fun smoothPath(corners: List<Offset>): Path {
    val path = Path()
    val start = (corners.last() + corners.first()) / 2f
    path.moveTo(start.x, start.y)
    corners.forEachIndexed { index, corner ->
        val end = (corner + corners[(index + 1) % corners.size]) / 2f
        path.quadraticTo(corner.x, corner.y, end.x, end.y)
    }
    path.close()
    return path
}
