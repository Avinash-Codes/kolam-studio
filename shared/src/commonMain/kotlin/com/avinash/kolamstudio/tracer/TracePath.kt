package com.avinash.kolamstudio.tracer

import com.avinash.kolamstudio.model.Point
import kotlin.math.hypot

private const val SAMPLES_PER_CURVE = 8
private const val LOOKAHEAD = 1.5f
private const val REACH = 0.5f

data class Sample(
    val x: Float,
    val y: Float,
    val distance: Float,
)

class TracePath(
    loop: List<Point>,
) {
    val samples: List<Sample> =
        buildList {
            var x = (loop.last().x + loop.first().x) / 2f
            var y = (loop.last().y + loop.first().y) / 2f
            var distance = 0f
            add(Sample(x, y, distance))
            loop.forEachIndexed { index, corner ->
                val next = loop[(index + 1) % loop.size]
                val startX = x
                val startY = y
                val endX = (corner.x + next.x) / 2f
                val endY = (corner.y + next.y) / 2f
                for (step in 1..SAMPLES_PER_CURVE) {
                    val t = step.toFloat() / SAMPLES_PER_CURVE
                    val sampleX = quadratic(startX, corner.x.toFloat(), endX, t)
                    val sampleY = quadratic(startY, corner.y.toFloat(), endY, t)
                    distance += hypot(sampleX - x, sampleY - y)
                    x = sampleX
                    y = sampleY
                    add(Sample(x, y, distance))
                }
            }
        }

    val length: Float get() = samples.last().distance

    fun advance(
        progress: Float,
        x: Float,
        y: Float,
    ): Float {
        val nearest =
            samples
                .filter { it.distance in progress..progress + LOOKAHEAD }
                .minByOrNull { hypot(it.x - x, it.y - y) }
        return if (nearest != null && hypot(nearest.x - x, nearest.y - y) <= REACH) nearest.distance else progress
    }
}

private fun quadratic(
    start: Float,
    control: Float,
    end: Float,
    t: Float,
): Float = (1 - t) * (1 - t) * start + 2 * t * (1 - t) * control + t * t * end
