package com.avinash.kolamstudio.platform

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.write

actual fun readUrlFragment(): String? = null

actual fun writeUrlFragment(fragment: String) = Unit

actual suspend fun savePng(
    bytes: ByteArray,
    name: String,
): Boolean {
    val file = FileKit.openFileSaver(suggestedName = name, defaultExtension = "png") ?: return false
    file.write(bytes)
    return true
}
