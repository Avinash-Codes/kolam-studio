package com.avinash.kolamstudio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.avinash.kolamstudio.generator.OFFERED_SIZES
import com.avinash.kolamstudio.generator.Symmetry
import com.avinash.kolamstudio.model.Pattern
import kotlin.math.roundToInt

private val WIDE_LAYOUT_MIN_WIDTH = 600.dp
private val PANEL_WIDTH = 360.dp
private val SHEET_PEEK_HEIGHT = 160.dp
private val CONTROL_ROW_HEIGHT = 48.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Workspace(
    state: WorkspaceState,
    pattern: Pattern,
    canvasLayer: GraphicsLayer,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier) {
        if (maxWidth >= WIDE_LAYOUT_MIN_WIDTH) {
            Row(
                Modifier.fillMaxSize().safeContentPadding().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                WorkspaceCanvas(state, pattern, canvasLayer, Modifier.weight(1f).fillMaxHeight())
                Column(
                    Modifier.width(PANEL_WIDTH).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PrimaryControls(state)
                    SecondaryControls(state)
                    actions()
                }
            }
        } else {
            val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            BottomSheetScaffold(
                sheetPeekHeight = SHEET_PEEK_HEIGHT + navigationBarHeight,
                sheetContent = {
                    Column(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PrimaryControls(state)
                        Spacer(Modifier.height(navigationBarHeight))
                        SecondaryControls(state)
                        actions()
                    }
                },
            ) { sheetPadding ->
                WorkspaceCanvas(
                    state,
                    pattern,
                    canvasLayer,
                    Modifier
                        .fillMaxSize()
                        .padding(sheetPadding)
                        .safeContentPadding()
                        .padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun WorkspaceCanvas(
    state: WorkspaceState,
    pattern: Pattern,
    canvasLayer: GraphicsLayer,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val tracing = state.mode == Mode.TRACE && state.tracePath != null
    val traceComplete = tracing && state.tracedFraction >= 1f
    LaunchedEffect(traceComplete) {
        if (traceComplete) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }
    KolamCanvas(
        pattern,
        modifier
            .drawWithContent {
                canvasLayer.record { this@drawWithContent.drawContent() }
                drawLayer(canvasLayer)
            }.background(MaterialTheme.colorScheme.surface),
        animate = state.mode == Mode.EXPLORE,
        showGraph = state.showGraph,
        onMidpointTap =
            if (state.mode == Mode.EDIT) {
                {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    state.tapMidpoint(it)
                }
            } else {
                null
            },
        traceFraction = if (tracing) state.tracedFraction else null,
        onTrace = if (tracing) state::traceTo else null,
    )
}

@Composable
private fun PrimaryControls(state: WorkspaceState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Mode.entries.forEach {
            FilterChip(
                selected = state.mode == it,
                enabled = it != Mode.TRACE || state.tracePath != null,
                onClick = { state.mode = it },
                label = { Text(it.label) },
            )
        }
        FilterChip(
            selected = state.showGraph,
            onClick = { state.showGraph = !state.showGraph },
            label = { Text("Graph") },
        )
    }
    Box(Modifier.heightIn(min = CONTROL_ROW_HEIGHT), contentAlignment = Alignment.Center) {
        when (state.mode) {
            Mode.EXPLORE -> {
                Button(onClick = state::newPattern) { Text("New pattern") }
            }

            Mode.EDIT -> {
                val loopCount = state.loops.size
                Text(if (loopCount == 1) "1 loop: a valid kolam" else "$loopCount loops: a kolam needs exactly 1")
            }

            Mode.TRACE -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val percent = (100 * state.tracedFraction).toInt()
                    Text(if (percent == 100) "Complete!" else "Traced $percent%")
                    TextButton(onClick = state::restartTrace) { Text("Start again") }
                }
            }
        }
    }
}

@Composable
private fun SecondaryControls(state: WorkspaceState) {
    when (state.mode) {
        Mode.EXPLORE -> {
            SizeSlider("Rows", state.rows) { state.resize(rows = it, cols = state.cols) }
            SizeSlider("Columns", state.cols) { state.resize(rows = state.rows, cols = it) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.symmetry == Symmetry.ONE_AXIS,
                    onClick = { state.chooseSymmetry(Symmetry.ONE_AXIS) },
                    label = { Text("One mirror") },
                )
                FilterChip(
                    selected = state.symmetry == Symmetry.TWO_AXES,
                    enabled = Symmetry.TWO_AXES in state.symmetries,
                    onClick = { state.chooseSymmetry(Symmetry.TWO_AXES) },
                    label = { Text("Two mirrors") },
                )
            }
        }

        Mode.EDIT -> {
            Text("Tap between two dots to change how the line meets there", textAlign = TextAlign.Center)
        }

        Mode.TRACE -> {
            Text("Start at the marker and follow the line", textAlign = TextAlign.Center)
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
