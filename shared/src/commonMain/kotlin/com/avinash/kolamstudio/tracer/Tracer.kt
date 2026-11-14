package com.avinash.kolamstudio.tracer

import com.avinash.kolamstudio.model.MidpointState
import com.avinash.kolamstudio.model.Pattern
import com.avinash.kolamstudio.model.Point

private val DIRECTIONS = intArrayOf(-1, 1)

/**
 * Walks the line of [pattern] and returns its closed loops, each as the ordered
 * list of points it visits (the last point connects back to the first).
 *
 * A "strand" here is one unit step: leaving point (x, y) in direction (dx, dy).
 * Each step has exactly one next step and exactly one previous step, so
 * following steps can never get stuck or merge into another path: it always
 * comes back to where it started. That is why the inner loop needs no limit.
 */
fun trace(pattern: Pattern): List<List<Point>> {
    val grid = pattern.grid
    val visited = BooleanArray((grid.width + 1) * (grid.height + 1) * 4)

    fun strand(
        x: Int,
        y: Int,
        dx: Int,
        dy: Int,
    ): Int = ((y * (grid.width + 1) + x) * 4) + (dx + 1) + (dy + 1) / 2

    val loops = mutableListOf<List<Point>>()
    for (startY in 0..grid.height) {
        for (startX in 0..grid.width) {
            if ((startX + startY) % 2 == 0) continue
            for (startDx in DIRECTIONS) {
                for (startDy in DIRECTIONS) {
                    val leavesBox = startX + startDx !in 0..grid.width || startY + startDy !in 0..grid.height
                    if (leavesBox || visited[strand(x = startX, y = startY, dx = startDx, dy = startDy)]) continue

                    val loop = mutableListOf<Point>()
                    var x = startX
                    var y = startY
                    var dx = startDx
                    var dy = startDy
                    do {
                        loop += Point(x, y)
                        visited[strand(x = x, y = y, dx = dx, dy = dy)] = true
                        x += dx
                        y += dy
                        // Also mark the same step walked backwards, so the loop is not traced again in reverse.
                        visited[strand(x = x, y = y, dx = -dx, dy = -dy)] = true
                        when {
                            x == 0 || x == grid.width -> {
                                dx = -dx
                            }

                            y == 0 || y == grid.height -> {
                                dy = -dy
                            }

                            else -> {
                                when (pattern.stateAt(x = x, y = y)) {
                                    MidpointState.CROSS -> Unit
                                    MidpointState.MIRROR_H -> dy = -dy
                                    MidpointState.MIRROR_V -> dx = -dx
                                }
                            }
                        }
                    } while (x != startX || y != startY || dx != startDx || dy != startDy)
                    loops += loop
                }
            }
        }
    }
    return loops
}
