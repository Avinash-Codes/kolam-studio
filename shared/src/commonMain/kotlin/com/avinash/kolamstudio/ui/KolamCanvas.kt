package com.avinash.kolamstudio.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.avinash.kolamstudio.model.Pattern
import com.avinash.kolamstudio.model.Point
import com.avinash.kolamstudio.tracer.trace
import kotlin.math.min

/** Draws the dots of [pattern] and its line as a raw zigzag, scaled to fit and centred. */
@Composable
fun KolamCanvas(
    pattern: Pattern,
    modifier: Modifier = Modifier,
) {
    val loops = remember(pattern) { trace(pattern) }
    val lineColor = MaterialTheme.colorScheme.primary
    val dotColor = MaterialTheme.colorScheme.onSurface
    Canvas(modifier) {
        val grid = pattern.grid
        // Pixels per grid unit: the largest scale at which the whole box still fits.
        val unit = min(size.width / grid.width, size.height / grid.height)
        val left = (size.width - unit * grid.width) / 2
        val top = (size.height - unit * grid.height) / 2

        fun Point.toOffset() = Offset(left + x * unit, top + y * unit)

        for (loop in loops) {
            val path = Path()
            loop.forEachIndexed { index, point ->
                val offset = point.toOffset()
                if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
            }
            path.close()
            drawPath(path, lineColor, style = Stroke(width = unit / 8, join = StrokeJoin.Round))
        }
        for (row in 0 until grid.rows) {
            for (col in 0 until grid.cols) {
                drawCircle(dotColor, radius = unit / 6, center = grid.dot(row, col).toOffset())
            }
        }
    }
}
