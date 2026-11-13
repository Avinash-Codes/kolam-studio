package com.avinash.kolamstudio

import androidx.compose.foundation.background
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.avinash.kolamstudio.generator.OFFERED_SIZES
import com.avinash.kolamstudio.generator.Symmetry
import com.avinash.kolamstudio.generator.generate
import com.avinash.kolamstudio.generator.offeredSymmetries
import com.avinash.kolamstudio.model.Grid
import com.avinash.kolamstudio.model.Pattern
import com.avinash.kolamstudio.model.patternFromCode
import com.avinash.kolamstudio.model.toCode
import com.avinash.kolamstudio.platform.readUrlFragment
import com.avinash.kolamstudio.platform.savePng
import com.avinash.kolamstudio.platform.writeUrlFragment
import com.avinash.kolamstudio.store.CollectionStore
import com.avinash.kolamstudio.tracer.TracePath
import com.avinash.kolamstudio.tracer.trace
import com.avinash.kolamstudio.ui.CollectionScreen
import com.avinash.kolamstudio.ui.KolamCanvas
import com.russhwolf.settings.Settings
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.dialogs.compose.util.encodeToByteArray
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class Mode(
    val label: String,
) {
    EXPLORE("Explore"),
    EDIT("Edit"),
    TRACE("Trace"),
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            val store = remember { CollectionStore(Settings()) }
            val saved by store.patterns.collectAsState()
            val scope = rememberCoroutineScope()
            val canvasLayer = rememberGraphicsLayer()
            val canvasBackground = MaterialTheme.colorScheme.surface
            var showCollection by remember { mutableStateOf(false) }
            var mode by remember { mutableStateOf(Mode.EXPLORE) }
            var showGraph by remember { mutableStateOf(false) }
            var pattern by remember { mutableStateOf(readUrlFragment()?.let(::offeredPatternFromCode)) }
            var rows by remember { mutableIntStateOf(pattern?.grid?.rows ?: 5) }
            var cols by remember { mutableIntStateOf(pattern?.grid?.cols ?: 5) }
            var wantedSymmetry by remember { mutableStateOf(Symmetry.TWO_AXES) }
            var generation by remember { mutableIntStateOf(0) }
            var status by remember(pattern) { mutableStateOf("") }

            val grid = Grid(rows, cols)
            val symmetries = offeredSymmetries(grid)
            val symmetry = if (wantedSymmetry in symmetries) wantedSymmetry else symmetries.first()
            LaunchedEffect(generation) {
                if (generation > 0 || pattern == null) generate(grid, symmetry).collect { pattern = it }
            }
            LaunchedEffect(pattern) { pattern?.let { writeUrlFragment(it.toCode()) } }

            val screenModifier = Modifier.fillMaxSize().safeContentPadding().padding(16.dp)
            val current = pattern ?: return@Surface
            if (showCollection) {
                CollectionScreen(
                    saved = saved,
                    currentCode = current.toCode(),
                    onOpenCode = { code ->
                        offeredPatternFromCode(code)?.let {
                            pattern = it
                            rows = it.grid.rows
                            cols = it.grid.cols
                            showCollection = false
                        }
                    },
                    onRename = store::rename,
                    onDelete = store::delete,
                    onBack = { showCollection = false },
                    modifier = screenModifier,
                )
                return@Surface
            }

            Column(
                screenModifier,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val loops = remember(current) { trace(current) }
                val tracePath = remember(loops) { loops.singleOrNull()?.let(::TracePath) }
                var traced by remember(current) { mutableFloatStateOf(0f) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Mode.entries.forEach {
                        FilterChip(
                            selected = mode == it,
                            enabled = it != Mode.TRACE || tracePath != null,
                            onClick = { mode = it },
                            label = { Text(it.label) },
                        )
                    }
                    FilterChip(selected = showGraph, onClick = { showGraph = !showGraph }, label = { Text("Graph") })
                }
                Row {
                    TextButton(
                        onClick = {
                            val name = "Kolam ${saved.size + 1}"
                            store.save(name, current)
                            status = "Saved as $name"
                        },
                    ) { Text("Save") }
                    TextButton(
                        onClick = {
                            scope.launch {
                                status =
                                    try {
                                        val png = canvasLayer.toImageBitmap().encodeToByteArray(ImageFormat.PNG)
                                        if (savePng(png, "kolam")) "Image saved" else ""
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (_: Exception) {
                                        "Could not save the image"
                                    }
                            }
                        },
                    ) { Text("Export image") }
                    TextButton(onClick = { showCollection = true }) { Text("Collection (${saved.size})") }
                }
                if (status.isNotEmpty()) Text(status)
                KolamCanvas(
                    current,
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .drawWithContent {
                            canvasLayer.record { this@drawWithContent.drawContent() }
                            drawLayer(canvasLayer)
                        }.background(canvasBackground),
                    animate = mode == Mode.EXPLORE,
                    showGraph = showGraph,
                    onMidpointTap = if (mode == Mode.EDIT) ({ pattern = current.withNextStateAt(it) }) else null,
                    traceFraction = if (mode == Mode.TRACE && tracePath != null) traced / tracePath.length else null,
                    onTrace = if (mode == Mode.TRACE && tracePath != null) ({ x, y -> traced = tracePath.advance(traced, x, y) }) else null,
                )
                when (mode) {
                    Mode.EXPLORE -> {
                        SizeSlider("Rows", rows) {
                            if (it != rows) {
                                rows = it
                                generation++
                            }
                        }
                        SizeSlider("Columns", cols) {
                            if (it != cols) {
                                cols = it
                                generation++
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = symmetry == Symmetry.ONE_AXIS,
                                onClick = {
                                    wantedSymmetry = Symmetry.ONE_AXIS
                                    generation++
                                },
                                label = { Text("One mirror") },
                            )
                            FilterChip(
                                selected = symmetry == Symmetry.TWO_AXES,
                                enabled = Symmetry.TWO_AXES in symmetries,
                                onClick = {
                                    wantedSymmetry = Symmetry.TWO_AXES
                                    generation++
                                },
                                label = { Text("Two mirrors") },
                            )
                        }
                        Button(onClick = { generation++ }) { Text("New pattern") }
                    }

                    Mode.EDIT -> {
                        Text(if (loops.size == 1) "1 loop: a valid kolam" else "${loops.size} loops: a kolam needs exactly 1")
                        Text("Tap between two dots to change how the line meets there", textAlign = TextAlign.Center)
                    }

                    Mode.TRACE -> {
                        val percent = if (tracePath == null) 0 else (100 * traced / tracePath.length).toInt()
                        Text(if (percent == 100) "Complete!" else "Traced $percent%")
                        Text("Start at the marker and follow the line", textAlign = TextAlign.Center)
                        Button(onClick = { traced = 0f }) { Text("Start again") }
                    }
                }
            }
        }
    }
}

private fun offeredPatternFromCode(code: String): Pattern? =
    patternFromCode(code)?.takeIf { it.grid.rows in OFFERED_SIZES && it.grid.cols in OFFERED_SIZES }

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
