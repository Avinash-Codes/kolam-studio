package com.avinash.kolamstudio.model

import kotlin.math.abs

/**
 * A grid of [rows] × [cols] dots.
 *
 * Dot (row, col) sits at point (2·col + 1, 2·row + 1), so the bounding box is
 * x in 0..[width] and y in 0..[height].
 */
data class Grid(
    val rows: Int,
    val cols: Int,
) {
    init {
        require(rows > 0 && cols > 0) { "A grid needs at least one dot, got $rows × $cols" }
    }

    val width: Int get() = 2 * cols
    val height: Int get() = 2 * rows

    /** Number of midpoints: one between every pair of neighbouring dots. */
    val midpointCount: Int get() = 2 * rows * cols - rows - cols

    /** Every midpoint, ordered by y then x. A point's position in this list is its [midpointIndex]. */
    val midpoints: List<Point>
        get() =
            (1 until height).flatMap { y ->
                (1 until width).filter { x -> (x + y) % 2 == 1 }.map { x -> Point(x, y) }
            }

    fun dot(
        row: Int,
        col: Int,
    ): Point = Point(2 * col + 1, 2 * row + 1)

    fun midpointAt(
        x: Float,
        y: Float,
    ): Point? = midpoints.firstOrNull { abs(it.x - x) + abs(it.y - y) < 1 }

    /**
     * Position of midpoint ([x], [y]) in [midpoints].
     *
     * Number all interior points row by row: n = (y - 1) · (width - 1) + (x - 1).
     * A row holds an odd number of points (width - 1), so the parity of x + y
     * alternates along the whole numbering like a chessboard, and the midpoints
     * (x + y odd) are exactly the odd n. Halving n therefore counts only them.
     */
    fun midpointIndex(
        x: Int,
        y: Int,
    ): Int = ((y - 1) * (width - 1) + (x - 1)) / 2
}
