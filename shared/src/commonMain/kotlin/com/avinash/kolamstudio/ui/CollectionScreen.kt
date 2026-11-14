package com.avinash.kolamstudio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.avinash.kolamstudio.model.patternFromCode
import com.avinash.kolamstudio.store.SavedPattern

@Composable
internal fun CollectionScreen(
    saved: List<SavedPattern>,
    currentCode: String,
    shareLink: String?,
    onOpenCode: (String) -> Boolean,
    onRename: (index: Int, name: String) -> Unit,
    onDelete: (index: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var codeText by remember { mutableStateOf("") }
    var invalidCode by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Int?>(null) }
    val pendingItem = pendingDelete?.let(saved::getOrNull)
    if (pendingItem != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${pendingItem.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete?.let(onDelete)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 680.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onBack) { Text("Back") }
                    Text("Collection", style = MaterialTheme.typography.headlineSmall)
                }
            }
            item {
                CopyableField(label = "Current pattern code", value = currentCode)
                if (shareLink != null) CopyableField(label = "Pattern link", value = shareLink)
            }
            item {
                OutlinedTextField(
                    value = codeText,
                    onValueChange = {
                        codeText = it
                        invalidCode = false
                    },
                    label = { Text("Paste a pattern code") },
                    isError = invalidCode,
                    supportingText =
                        if (invalidCode) {
                            { Text("Use a valid KolamStudio code for a grid of 2–8 rows and columns.") }
                        } else {
                            null
                        },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Paste a pattern code" },
                )
                Button(
                    onClick = { invalidCode = !onOpenCode(codeText.trim()) },
                    enabled = codeText.isNotBlank(),
                    modifier = Modifier.padding(top = 6.dp),
                ) { Text("Import") }
            }
            item {
                Text("Saved patterns", style = MaterialTheme.typography.titleMedium)
                if (saved.isEmpty()) Text("Use Save in the workspace to add a pattern here.")
            }
            itemsIndexed(saved) { index, item ->
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        patternFromCode(item.code)?.let { KolamCanvas(it, Modifier.size(48.dp), animate = false) }
                        OutlinedTextField(
                            value = item.name,
                            onValueChange = { onRename(index, it) },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onOpenCode(item.code) }) { Text("Open") }
                        TextButton(onClick = { pendingDelete = index }) { Text("Delete") }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CopyableField(
    label: String,
    value: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
