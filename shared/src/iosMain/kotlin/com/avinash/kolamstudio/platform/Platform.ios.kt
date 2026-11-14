package com.avinash.kolamstudio.platform

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.dialogs.shareFile
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.write

internal actual fun readShareLink(fragment: String): String? = null

internal actual fun readUrlFragment(): String? = null

internal actual fun writeUrlFragment(fragment: String) = Unit

internal actual suspend fun savePng(
    bytes: ByteArray,
    name: String,
): Boolean {
    val file = FileKit.openFileSaver(suggestedName = name, defaultExtension = "png") ?: return false
    file.write(bytes)
    return true
}

internal actual val canShareImages: Boolean = true

internal actual suspend fun sharePng(
    bytes: ByteArray,
    name: String,
) {
    val file = FileKit.cacheDir / "$name.png"
    file.write(bytes)
    FileKit.shareFile(file)
}
