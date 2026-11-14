package com.avinash.kolamstudio.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import com.avinash.kolamstudio.model.Grid
import com.avinash.kolamstudio.model.MidpointState
import com.avinash.kolamstudio.model.Pattern
import com.avinash.kolamstudio.model.Point
import com.avinash.kolamstudio.tracer.trace
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.dialogs.compose.util.encodeToByteArray
import kotlin.math.max
import kotlin.math.min

private const val DRAW_MILLIS = 2500
private const val FIRST_LOOP_HUE = 10f
private const val EXPORT_LONG_EDGE = 1200
private const val GOLDEN_ANGLE = 137.5f
private const val WALL_HALF_LENGTH = 0.35f
private const val LOOP_BRIGHTNESS_ON_LIGHT = 0.65f
private const val LOOP_BRIGHTNESS_ON_DARK = 0.9f

@Composable
internal fun KolamCanvas(
    pattern: Pattern,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    showGraph: Boolean = false,
    highlightedMidpoint: Point? = null,
    affectedLoops: Set<Int> = emptySet(),
    onMidpointTap: ((Point) -> Unit)? = null,
    traceFraction: Float? = null,
    onTrace: ((x: Float, y: Float) -> Unit)? = null,
) {
    val grid = pattern.grid
    val loops = remember(pattern) { trace(pattern) }
    val progress = remember(pattern, animate) { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(progress) { progress.animateTo(1f, tween(DRAW_MILLIS, easing = LinearEasing)) }
    val dotColor = MaterialTheme.colorScheme.onSurface
    val highlightColor = MaterialTheme.colorScheme.primary
    val paperColor = MaterialTheme.colorScheme.surfaceContainerLow
    val graphColor = MaterialTheme.colorScheme.outline
    val headColor = MaterialTheme.colorScheme.tertiary
    val onDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val loopBrightness = if (onDarkSurface) LOOP_BRIGHTNESS_ON_DARK else LOOP_BRIGHTNESS_ON_LIGHT
    val currentOnMidpointTap by rememberUpdatedState(onMidpointTap)
    val currentOnTrace by rememberUpdatedState(onTrace)
    val pointerModifier =
        Modifier.pointerInput(grid, onMidpointTap != null, onTrace != null) {
            fun gridPosition(pixel: Offset): Offset {
                val frame = GridFrame(grid, size.toSize())
                return (pixel - frame.origin) / frame.unit
            }
            if (onMidpointTap != null) {
                detectTapGestures { tap ->
                    val position = gridPosition(tap)
                    grid.midpointAt(x = position.x, y = position.y)?.let { currentOnMidpointTap?.invoke(it) }
                }
            } else if (onTrace != null) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    gridPosition(down.position).let { currentOnTrace?.invoke(it.x, it.y) }
                    drag(down.id) { change ->
                        gridPosition(change.position).let { currentOnTrace?.invoke(it.x, it.y) }
                        change.consume()
                    }
                }
            }
        }
    Spacer(
        modifier
            .semantics { contentDescription = "${grid.rows} by ${grid.cols} dot kolam, ${loops.size} closed loops" }
            .then(pointerModifier)
            .drawWithCache {
                val frame = GridFrame(grid, size)
                val unit = frame.unit
                val corners = loops.map { loop -> loop.map(frame::toOffset) }
                val curves = corners.map(::smoothPath)
                val zigzags = if (showGraph) corners.map(::closedPolyline) else emptyList()
                val regularStroke = loopStroke(unit)
                val emphasizedStroke = loopStroke(unit * 1.16f)
                val measure = PathMeasure()
                val drawn = Path()
                onDrawBehind {
                    if (showGraph) zigzags.forEach { drawPath(it, graphColor, style = Stroke(width = unit / 32)) }
                    curves.forEachIndexed { index, curve ->
                        val emphasis = index in affectedLoops
                        val opacity = if (affectedLoops.isEmpty() || emphasis) 1f else 0.4f
                        val color = loopColor(index, loopBrightness).copy(alpha = opacity)
                        val stroke = if (emphasis) emphasizedStroke else regularStroke
                        val fraction = traceFraction ?: progress.value
                        if (fraction >= 1f && traceFraction == null) {
                            drawPath(curve, color, style = stroke)
                        } else {
                            measure.setPath(curve, forceClosed = false)
                            val drawnLength = measure.length * fraction
                            drawn.rewind()
                            if (traceFraction != null) drawPath(curve, color.copy(alpha = 0.25f), style = stroke)
                            if (measure.getSegment(startDistance = 0f, stopDistance = drawnLength, destination = drawn)) {
                                drawPath(drawn, color, style = stroke)
                            }
                            if (traceFraction != null) {
                                drawCircle(headColor, radius = unit / 5, center = measure.getPosition(drawnLength))
                            }
                        }
                    }
                    if (showGraph) {
                        for (midpoint in grid.midpoints) {
                            val center = frame.toOffset(midpoint)
                            val state = pattern.stateAt(x = midpoint.x, y = midpoint.y)
                            drawCircle(paperColor, radius = unit / 7, center = center)
                            drawMidpoint(state, center, unit, graphColor)
                        }
                    }
                    if (highlightedMidpoint != null) {
                        val center = frame.toOffset(highlightedMidpoint)
                        drawCircle(paperColor, radius = unit / 3.2f, center = center)
                        drawCircle(highlightColor, radius = unit / 3.2f, center = center, style = Stroke(unit / 20))
                        val state = pattern.stateAt(x = highlightedMidpoint.x, y = highlightedMidpoint.y)
                        drawMidpoint(state, center, unit * 0.62f, highlightColor)
                    }
                    drawDots(grid, frame, dotColor)
                }
            },
    )
}

/** Renders the finished pattern on paper at a size independent of the workspace. */
internal suspend fun renderPatternPng(pattern: Pattern): ByteArray {
    val grid = pattern.grid
    val scale = EXPORT_LONG_EDGE.toFloat() / max(grid.width, grid.height)
    val width = (grid.width * scale).toInt()
    val height = (grid.height * scale).toInt()
    val bitmap = ImageBitmap(width, height)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(width.toFloat(), height.toFloat())) {
        drawRect(Color(0xFFFFFBF4))
        val frame = GridFrame(grid, size)
        trace(pattern).forEachIndexed { index, loop ->
            drawPath(smoothPath(loop.map(frame::toOffset)), loopColor(index, LOOP_BRIGHTNESS_ON_LIGHT), style = loopStroke(frame.unit))
        }
        drawDots(grid, frame, Color(0xFF2A211B))
    }
    return bitmap.encodeToByteArray(ImageFormat.PNG)
}

private fun loopStroke(unit: Float) = Stroke(width = unit / 8, cap = StrokeCap.Round, join = StrokeJoin.Round)

private fun DrawScope.drawDots(
    grid: Grid,
    frame: GridFrame,
    color: Color,
) {
    for (row in 0 until grid.rows) {
        for (col in 0 until grid.cols) {
            drawCircle(color, radius = frame.unit / 8, center = frame.toOffset(grid.dot(row = row, col = col)))
        }
    }
}

private class GridFrame(
    grid: Grid,
    size: Size,
) {
    val unit = min(size.width / (grid.width + 0.8f), size.height / (grid.height + 0.8f))
    val origin = Offset((size.width - unit * grid.width) / 2, (size.height - unit * grid.height) / 2)

    fun toOffset(point: Point) = origin + Offset(point.x * unit, point.y * unit)
}

private fun loopColor(
    index: Int,
    brightness: Float,
) = Color.hsv((FIRST_LOOP_HUE + index * GOLDEN_ANGLE) % 360f, 0.55f, brightness)

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
