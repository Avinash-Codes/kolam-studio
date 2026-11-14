package com.avinash.kolamstudio

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.rememberGraphicsLayer
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
import com.avinash.kolamstudio.platform.readUrlFragment
import com.avinash.kolamstudio.platform.savePng
import com.avinash.kolamstudio.platform.sharePng
import com.avinash.kolamstudio.platform.writeUrlFragment
import com.avinash.kolamstudio.store.CollectionStore
import com.avinash.kolamstudio.ui.CollectionScreen
import com.avinash.kolamstudio.ui.Workspace
import com.avinash.kolamstudio.ui.WorkspaceState
import com.russhwolf.settings.Settings
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.dialogs.compose.util.encodeToByteArray
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            val store = remember { CollectionStore(Settings()) }
            val saved by store.patterns.collectAsState()
            val state = remember { WorkspaceState(readUrlFragment()?.let(::offeredPatternFromCode)) }
            val scope = rememberCoroutineScope()
            val canvasLayer = rememberGraphicsLayer()
            var showCollection by remember { mutableStateOf(false) }

            LaunchedEffect(state.generation) {
                if (state.generation > 0 || state.pattern == null) generate(state.grid, state.symmetry).collect(state::show)
            }
            val pattern = state.pattern ?: return@Surface
            LaunchedEffect(pattern) { writeUrlFragment(pattern.toCode()) }
            NavigationBackHandler(
                state = rememberNavigationEventState(NavigationEventInfo.None),
                isBackEnabled = showCollection,
                onBackCompleted = { showCollection = false },
            )

            if (showCollection) {
                CollectionScreen(
                    saved = saved,
                    currentCode = pattern.toCode(),
                    onOpenCode = { code ->
                        offeredPatternFromCode(code)?.let {
                            state.show(it)
                            showCollection = false
                        }
                    },
                    onRename = store::rename,
                    onDelete = store::delete,
                    onBack = { showCollection = false },
                    modifier = Modifier.fillMaxSize().safeContentPadding().padding(16.dp),
                )
                return@Surface
            }

            fun withCanvasPng(use: suspend (ByteArray) -> String) {
                scope.launch {
                    state.status =
                        try {
                            use(canvasLayer.toImageBitmap().encodeToByteArray(ImageFormat.PNG))
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            "Could not export the image"
                        }
                }
            }

            Workspace(state, pattern, canvasLayer) {
                FlowRow(horizontalArrangement = Arrangement.Center) {
                    TextButton(
                        onClick = {
                            val name = "Kolam ${saved.size + 1}"
                            store.save(name, pattern)
                            state.status = "Saved as $name"
                        },
                    ) { Text("Save") }
                    TextButton(onClick = { showCollection = true }) { Text("Collection (${saved.size})") }
                    TextButton(onClick = { withCanvasPng { if (savePng(it, IMAGE_NAME)) "Image saved" else "" } }) {
                        Text("Export image")
                    }
                    if (canShareImages) {
                        TextButton(
                            onClick = {
                                withCanvasPng {
                                    sharePng(it, IMAGE_NAME)
                                    ""
                                }
                            },
                        ) { Text("Share") }
                    }
                }
                if (state.status.isNotEmpty()) Text(state.status)
            }
        }
    }
}

private const val IMAGE_NAME = "kolam"

private fun offeredPatternFromCode(code: String): Pattern? =
    patternFromCode(code)?.takeIf { it.grid.rows in OFFERED_SIZES && it.grid.cols in OFFERED_SIZES }
