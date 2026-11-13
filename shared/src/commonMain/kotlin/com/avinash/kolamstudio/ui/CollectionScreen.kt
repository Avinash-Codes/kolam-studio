package com.avinash.kolamstudio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.avinash.kolamstudio.model.patternFromCode
import com.avinash.kolamstudio.store.SavedPattern

@Composable
fun CollectionScreen(
    saved: List<SavedPattern>,
    currentCode: String,
    onOpenCode: (String) -> Unit,
    onRename: (index: Int, name: String) -> Unit,
    onDelete: (index: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Collection", style = MaterialTheme.typography.titleLarge)
        }
        var codeText by remember(currentCode) { mutableStateOf(currentCode) }
        OutlinedTextField(
            value = codeText,
            onValueChange = {
                codeText = it
                onOpenCode(it.trim())
            },
            label = { Text("Pattern code") },
            supportingText = { Text("Copy this code to share the pattern, or paste another code to open it") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (saved.isEmpty()) Text("No saved patterns yet. Press Save on the workspace to add one.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(saved) { index, item ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    patternFromCode(item.code)?.let { KolamCanvas(it, Modifier.size(56.dp), animate = false) }
                    var name by remember(item) { mutableStateOf(item.name) }
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            onRename(index, it)
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onOpenCode(item.code) }) { Text("Open") }
                    TextButton(onClick = { onDelete(index) }) { Text("Delete") }
                }
            }
        }
    }
}
