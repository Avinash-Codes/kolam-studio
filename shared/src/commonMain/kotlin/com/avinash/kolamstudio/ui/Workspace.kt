package com.avinash.kolamstudio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.avinash.kolamstudio.generator.OFFERED_SIZES
import com.avinash.kolamstudio.generator.Symmetry
import com.avinash.kolamstudio.model.MidpointState
import kotlin.math.roundToInt

private val WideLayoutMinWidth = 650.dp
private val PanelWidth = 340.dp

@Composable
internal fun Workspace(
    state: WorkspaceState,
    onLessons: () -> Unit,
    actions: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().safeContentPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "KolamStudio",
                Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
            )
            TextButton(onClick = onLessons) { Text("Lessons") }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            if (maxWidth >= WideLayoutMinWidth) {
                Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    WorkspaceCanvas(state, Modifier.weight(1f).fillMaxHeight())
                    Column(
                        Modifier.width(PanelWidth).fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        WorkspaceControls(state)
                        HorizontalDivider()
                        actions()
                    }
                }
            } else {
                val controlsHeight = (maxHeight * 0.46f).coerceAtMost(340.dp)
                Column(Modifier.fillMaxSize()) {
                    WorkspaceCanvas(state, Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = controlsHeight)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        WorkspaceControls(state)
                    }
                    HorizontalDivider()
                    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) { actions() }
                }
            }
        }
    }
}

@Composable
private fun WorkspaceCanvas(
    state: WorkspaceState,
    modifier: Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val tracing = state.mode == Mode.TRACE && state.tracePath != null
    val complete = tracing && state.tracedFraction >= 1f
    LaunchedEffect(complete) {
        if (complete) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }
    val shape = RoundedCornerShape(16.dp)
    KolamCanvas(
        pattern = state.pattern,
        modifier =
            modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        animate = state.mode == Mode.EXPLORE,
        showGraph = state.showGraph,
        highlightedMidpoint = if (state.mode == Mode.EDIT) state.highlightedMidpoint else null,
        affectedLoops = if (state.mode == Mode.EDIT) state.affectedLoops else emptySet(),
        onMidpointTap =
            if (state.mode == Mode.EDIT) {
                { midpoint ->
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    state.tapMidpoint(midpoint)
                }
            } else {
                null
            },
        traceFraction = if (tracing) state.tracedFraction else null,
        onTrace = if (tracing) state::traceTo else null,
    )
}

@Composable
private fun WorkspaceControls(state: WorkspaceState) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (mode in Mode.entries) {
            FilterChip(
                selected = state.mode == mode,
                enabled = mode != Mode.TRACE || state.tracePath != null,
                onClick = { state.selectMode(mode) },
                label = { Text(mode.label) },
            )
        }
        FilterChip(selected = state.showGraph, onClick = { state.showGraph = !state.showGraph }, label = { Text("Graph") })
    }
    when (state.mode) {
        Mode.EXPLORE -> ExploreControls(state)
        Mode.EDIT -> EditControls(state)
        Mode.TRACE -> TraceControls(state)
    }
}

@Composable
private fun ExploreControls(state: WorkspaceState) {
    var showSettings by remember { mutableStateOf(false) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = state::newPattern) { Text("New pattern") }
        TextButton(onClick = { showSettings = !showSettings }) { Text(if (showSettings) "Hide grid settings" else "Grid settings") }
    }
    if (showSettings) {
        SizeSlider("Rows", state.grid.rows) { state.resize(state.grid.copy(rows = it)) }
        SizeSlider("Columns", state.grid.cols) { state.resize(state.grid.copy(cols = it)) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (symmetry in Symmetry.entries) {
                FilterChip(
                    selected = state.symmetry == symmetry,
                    enabled = symmetry in state.symmetries,
                    onClick = { state.chooseSymmetry(symmetry) },
                    label = { Text(if (symmetry == Symmetry.ONE_AXIS) "One mirror" else "Two mirrors") },
                )
            }
        }
    }
}

@Composable
private fun EditControls(state: WorkspaceState) {
    val loopCount = state.loops.size
    val lesson = state.lesson
    val edit = state.lastEdit
    if (lesson != null) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(lesson.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (state.lessonComplete) lesson.result else lesson.description,
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    } else if (edit == null) {
        Text("Tap between two dots to cycle through a crossing and two mirrors.")
    }
    val feedback =
        if (edit == null) {
            if (loopCount == 1) "1 closed loop" else "$loopCount separate loops"
        } else {
            val piece =
                when (state.pattern.stateAt(x = edit.midpoint.x, y = edit.midpoint.y)) {
                    MidpointState.CROSS -> "Crossing"
                    MidpointState.MIRROR_H -> "Horizontal mirror"
                    MidpointState.MIRROR_V -> "Vertical mirror"
                }
            val effect =
                when {
                    loopCount < edit.previousLoops.size -> "joined loops"
                    loopCount > edit.previousLoops.size -> "split a loop"
                    else -> "rerouted a loop"
                }
            "$piece $effect (${edit.previousLoops.size} → $loopCount)."
        }
    Text(feedback, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.bodyMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.lessonComplete && state.tracePath != null) {
            Button(onClick = { state.selectMode(Mode.TRACE) }) { Text("Trace this loop") }
        } else if (lesson != null && !state.lessonComplete) {
            Button(onClick = { state.tapMidpoint(lesson.midpoint) }) { Text("Change crossing") }
        }
        if (edit != null) TextButton(onClick = state::undo) { Text("Undo") }
    }
}

@Composable
private fun TraceControls(state: WorkspaceState) {
    val progress = state.tracedFraction
    val complete = progress >= 1f
    Text(
        if (complete) "Loop complete" else "Traced ${(progress * 100).toInt()}%",
        Modifier.semantics { if (complete) liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.titleMedium,
    )
    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    Text(if (complete) "You drew the kolam in one stroke." else "Start at the marker and follow the curve to bring it back to the start.")
    TextButton(onClick = state::restartTrace) { Text("Start again") }
}

@Composable
private fun SizeSlider(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label $value", Modifier.width(84.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = OFFERED_SIZES.first.toFloat()..OFFERED_SIZES.last.toFloat(),
            steps = OFFERED_SIZES.count() - 2,
            modifier = Modifier.weight(1f),
        )
    }
}
