package com.avinash.kolamstudio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.avinash.kolamstudio.generator.OFFERED_SIZES
import com.avinash.kolamstudio.generator.Symmetry
import com.avinash.kolamstudio.generator.generate
import com.avinash.kolamstudio.generator.offeredSymmetries
import com.avinash.kolamstudio.model.Grid
import com.avinash.kolamstudio.model.Pattern
import com.avinash.kolamstudio.tracer.trace
import com.avinash.kolamstudio.ui.KolamCanvas
import kotlin.math.roundToInt

private enum class Mode(
    val label: String,
) {
    EXPLORE("Explore"),
    EDIT("Edit"),
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            var mode by remember { mutableStateOf(Mode.EXPLORE) }
            var showGraph by remember { mutableStateOf(false) }
            var rows by remember { mutableIntStateOf(5) }
            var cols by remember { mutableIntStateOf(5) }
            var wantedSymmetry by remember { mutableStateOf(Symmetry.TWO_AXES) }
            var generation by remember { mutableIntStateOf(0) }
            var pattern by remember { mutableStateOf<Pattern?>(null) }

            val grid = Grid(rows, cols)
            val symmetries = offeredSymmetries(grid)
            val symmetry = if (wantedSymmetry in symmetries) wantedSymmetry else symmetries.first()
            LaunchedEffect(grid, symmetry, generation) {
                generate(grid, symmetry).collect { pattern = it }
            }

            Column(
                Modifier.fillMaxSize().safeContentPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Mode.entries.forEach {
                        FilterChip(selected = mode == it, onClick = { mode = it }, label = { Text(it.label) })
                    }
                    FilterChip(selected = showGraph, onClick = { showGraph = !showGraph }, label = { Text("Graph view") })
                }
                val current = pattern ?: return@Column
                KolamCanvas(
                    current,
                    Modifier.weight(1f).fillMaxWidth(),
                    animate = mode == Mode.EXPLORE,
                    showGraph = showGraph,
                    onMidpointTap = if (mode == Mode.EDIT) ({ pattern = current.withNextStateAt(it) }) else null,
                )
                when (mode) {
                    Mode.EXPLORE -> {
                        SizeSlider("Rows", rows) { rows = it }
                        SizeSlider("Columns", cols) { cols = it }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = symmetry == Symmetry.ONE_AXIS,
                                onClick = { wantedSymmetry = Symmetry.ONE_AXIS },
                                label = { Text("One mirror") },
                            )
                            FilterChip(
                                selected = symmetry == Symmetry.TWO_AXES,
                                enabled = Symmetry.TWO_AXES in symmetries,
                                onClick = { wantedSymmetry = Symmetry.TWO_AXES },
                                label = { Text("Two mirrors") },
                            )
                        }
                        Button(onClick = { generation++ }) { Text("New pattern") }
                    }

                    Mode.EDIT -> {
                        val loopCount = remember(current) { trace(current).size }
                        Text(if (loopCount == 1) "1 loop: a valid kolam" else "$loopCount loops: a kolam needs exactly 1")
                        Text("Tap between two dots to change how the line meets there", textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun SizeSlider(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label $value", Modifier.width(96.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = OFFERED_SIZES.first.toFloat()..OFFERED_SIZES.last.toFloat(),
            steps = OFFERED_SIZES.count() - 2,
            modifier = Modifier.weight(1f),
        )
    }
}
