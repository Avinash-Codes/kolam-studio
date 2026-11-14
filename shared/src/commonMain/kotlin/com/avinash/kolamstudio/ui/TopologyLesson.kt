package com.avinash.kolamstudio.ui

import com.avinash.kolamstudio.model.Point
import com.avinash.kolamstudio.model.patternFromCode

internal enum class TopologyLesson(
    val title: String,
    val description: String,
    val result: String,
    val code: String,
    val midpoint: Point,
    val goalLoops: Int,
) {
    JOIN(
        title = "Join two loops",
        description = "Change the highlighted crossing to connect the two colored loops.",
        result = "Two loops became one. You can now draw the whole kolam in one stroke.",
        code = "AgIA",
        midpoint = Point(x = 2, y = 1),
        goalLoops = 1,
    ),
    SPLIT(
        title = "Split one loop",
        description = "Turn the highlighted crossing into a mirror. Watch the single loop split in two.",
        result = "The mirror redirected the strands into two separate loops. Undo to trace the original.",
        code = "AwQAAAAAAA",
        midpoint = Point(x = 1, y = 2),
        goalLoops = 2,
    ),
    REROUTE(
        title = "Reroute a loop",
        description = "Change the highlighted crossing. Can you change the route and keep one closed loop?",
        result = "The route changed, and it still forms a single loop. Trace the new route.",
        code = "AwOKAAA",
        midpoint = Point(x = 2, y = 3),
        goalLoops = 1,
    ),
    ;

    val fragment: String get() = "lesson/${name.lowercase()}"

    fun pattern() = checkNotNull(patternFromCode(code))
}
