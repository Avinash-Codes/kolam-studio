package com.avinash.kolamstudio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.avinash.kolamstudio.generator.OFFERED_SIZES
import com.avinash.kolamstudio.generator.generate
import com.avinash.kolamstudio.model.Pattern
import com.avinash.kolamstudio.model.patternFromCode
import com.avinash.kolamstudio.model.toCode
import com.avinash.kolamstudio.platform.canShareImages
import com.avinash.kolamstudio.platform.readShareLink
import com.avinash.kolamstudio.platform.readUrlFragment
import com.avinash.kolamstudio.platform.savePng
import com.avinash.kolamstudio.platform.sharePng
import com.avinash.kolamstudio.platform.writeUrlFragment
import com.avinash.kolamstudio.store.CollectionStore
import com.avinash.kolamstudio.ui.CollectionScreen
import com.avinash.kolamstudio.ui.LessonsScreen
import com.avinash.kolamstudio.ui.StudioTheme
import com.avinash.kolamstudio.ui.TopologyLesson
import com.avinash.kolamstudio.ui.Workspace
import com.avinash.kolamstudio.ui.WorkspaceState
import com.avinash.kolamstudio.ui.renderPatternPng
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val IMAGE_NAME = "kolam"

private enum class Screen { WORKSPACE, LESSONS, COLLECTION }

@Composable
@Preview
fun App() {
    StudioTheme {
        Surface(Modifier.fillMaxSize()) {
            val store = remember { CollectionStore(Settings()) }
            val saved by store.patterns.collectAsState()
            val state =
                remember {
                    val fragment = readUrlFragment()
                    val lesson = TopologyLesson.entries.firstOrNull { fragment == it.fragment }
                    WorkspaceState(fragment?.let(::offeredPatternFromCode), initialLesson = lesson ?: TopologyLesson.JOIN)
                }
            val scope = rememberCoroutineScope()
            var screen by remember { mutableStateOf(Screen.WORKSPACE) }
            var exporting by remember { mutableStateOf(false) }
            var showOutput by remember { mutableStateOf(false) }

            val request = state.request
            LaunchedEffect(request) {
                if (request != null) {
                    val (pattern, symmetry) = generate(request.grid, request.symmetry)
                    state.finishGeneration(request, pattern, symmetry)
                }
            }
            val fragment = state.urlFragment
            LaunchedEffect(fragment) { writeUrlFragment(fragment) }
            NavigationBackHandler(
                state = rememberNavigationEventState(NavigationEventInfo.None),
                isBackEnabled = screen != Screen.WORKSPACE,
                onBackCompleted = { screen = Screen.WORKSPACE },
            )
            val pageModifier = Modifier.fillMaxSize().safeContentPadding().padding(16.dp)
            val pattern = state.pattern

            fun export(use: suspend (ByteArray) -> String) {
                exporting = true
                scope.launch {
                    try {
                        state.status = use(renderPatternPng(pattern))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        state.status = "Could not export the image."
                    } finally {
                        exporting = false
                    }
                }
            }

            when (screen) {
                Screen.LESSONS -> {
                    LessonsScreen(
                        onOpen = {
                            state.openLesson(it)
                            screen = Screen.WORKSPACE
                        },
                        onBack = { screen = Screen.WORKSPACE },
                        modifier = pageModifier,
                    )
                }

                Screen.COLLECTION -> {
                    CollectionScreen(
                        saved = saved,
                        currentCode = pattern.toCode(),
                        shareLink = readShareLink(pattern.toCode()),
                        onOpenCode = { code ->
                            val imported = offeredPatternFromCode(code)
                            if (imported != null) {
                                state.openPattern(imported)
                                screen = Screen.WORKSPACE
                            }
                            imported != null
                        },
                        onRename = store::rename,
                        onDelete = store::delete,
                        onBack = { screen = Screen.WORKSPACE },
                        modifier = pageModifier,
                    )
                }

                Screen.WORKSPACE -> {
                    Workspace(state, onLessons = { screen = Screen.LESSONS }) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                val name = "Kolam ${saved.size + 1}"
                                store.save(name, pattern)
                                state.status = "Saved as $name"
                            }) { Text("Save") }
                            TextButton(onClick = { screen = Screen.COLLECTION }) { Text("Collection (${saved.size})") }
                            if (canShareImages) {
                                Column {
                                    TextButton(onClick = { showOutput = true }, enabled = !exporting) { Text("Export") }
                                    DropdownMenu(expanded = showOutput, onDismissRequest = { showOutput = false }) {
                                        DropdownMenuItem(text = { Text("Save PNG") }, onClick = {
                                            showOutput = false
                                            export { if (savePng(it, IMAGE_NAME)) "Image saved" else "" }
                                        })
                                        DropdownMenuItem(text = { Text("Share PNG") }, onClick = {
                                            showOutput = false
                                            export {
                                                sharePng(it, IMAGE_NAME)
                                                ""
                                            }
                                        })
                                    }
                                }
                            } else {
                                TextButton(
                                    onClick = { export { if (savePng(it, IMAGE_NAME)) "Image saved" else "" } },
                                    enabled = !exporting,
                                ) { Text("Export PNG") }
                            }
                        }
                        if (state.status.isNotEmpty()) {
                            Text(
                                state.status,
                                Modifier.semantics { liveRegion = LiveRegionMode.Polite }.padding(bottom = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun offeredPatternFromCode(code: String): Pattern? =
    patternFromCode(code.trim())?.takeIf { it.grid.rows in OFFERED_SIZES && it.grid.cols in OFFERED_SIZES }
