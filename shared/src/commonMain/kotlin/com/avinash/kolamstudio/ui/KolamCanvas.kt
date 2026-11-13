package com.avinash.kolamstudio.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.avinash.kolamstudio.model.Pattern
import com.avinash.kolamstudio.model.Point
import com.avinash.kolamstudio.tracer.trace
import kotlin.math.min

private const val DRAW_MILLIS = 2500

@Composable
fun KolamCanvas(
    pattern: Pattern,
    modifier: Modifier = Modifier,
) {
    val loops = remember(pattern) { trace(pattern) }
    val progress = remember(pattern) { Animatable(0f) }
    LaunchedEffect(pattern) { progress.animateTo(1f, tween(DRAW_MILLIS, easing = LinearEasing)) }
    val lineColor = MaterialTheme.colorScheme.primary
    val dotColor = MaterialTheme.colorScheme.onSurface
    Spacer(
        modifier.drawWithCache {
            val grid = pattern.grid
            val unit = min(size.width / grid.width, size.height / grid.height)
            val left = (size.width - unit * grid.width) / 2
            val top = (size.height - unit * grid.height) / 2

            fun Point.toOffset() = Offset(left + x * unit, top + y * unit)

            val paths = loops.map { loop -> smoothPath(loop.map { it.toOffset() }) }
            val dots = (0 until grid.rows).flatMap { row -> (0 until grid.cols).map { col -> grid.dot(row, col).toOffset() } }
            val measure = PathMeasure()
            val stroke = Stroke(width = unit / 8, cap = StrokeCap.Round, join = StrokeJoin.Round)
            onDrawBehind {
                for (path in paths) {
                    measure.setPath(path, forceClosed = false)
                    val drawn = Path()
                    measure.getSegment(0f, measure.length * progress.value, drawn)
                    drawPath(drawn, lineColor, style = stroke)
                }
                dots.forEach { drawCircle(dotColor, radius = unit / 6, center = it) }
            }
        },
    )
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
