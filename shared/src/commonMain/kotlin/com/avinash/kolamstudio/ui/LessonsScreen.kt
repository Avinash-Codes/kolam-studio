package com.avinash.kolamstudio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.avinash.kolamstudio.platform.readShareLink

@Composable
internal fun LessonsScreen(
    onOpen: (TopologyLesson) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = 680.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onBack) { Text("Back") }
                    Text("Kolam lessons", style = MaterialTheme.typography.headlineSmall)
                }
                Text("In sikku kolam, curves wind around a grid of dots. Each lesson changes how two strands meet.")
            }
            items(TopologyLesson.entries) { lesson ->
                val pattern = remember(lesson) { lesson.pattern() }
                OutlinedCard {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            KolamCanvas(pattern, Modifier.size(56.dp), animate = false)
                            Column(Modifier.weight(1f)) {
                                Text(lesson.title, style = MaterialTheme.typography.titleMedium)
                                Text(lesson.description, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        TextButton(onClick = { onOpen(lesson) }) { Text("Open lesson") }
                        val link = readShareLink(lesson.fragment)
                        if (link != null) CopyableField(label = "Lesson link", value = link)
                        CopyableField(label = "Starting pattern code", value = lesson.code)
                    }
                }
            }
        }
    }
}
