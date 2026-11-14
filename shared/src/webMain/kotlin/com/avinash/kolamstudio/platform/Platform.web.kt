@file:OptIn(ExperimentalWasmJsInterop::class)

package com.avinash.kolamstudio.platform

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.download
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js

actual fun readUrlFragment(): String? = locationHash().removePrefix("#").ifEmpty { null }

actual fun writeUrlFragment(fragment: String) = replaceLocationHash(fragment)

actual suspend fun savePng(
    bytes: ByteArray,
    name: String,
): Boolean {
    FileKit.download(bytes, fileName = "$name.png")
    return true
}

actual val canShareImages: Boolean = false

actual suspend fun sharePng(
    bytes: ByteArray,
    name: String,
) = Unit

private fun locationHash(): String = js("window.location.hash")

private fun replaceLocationHash(fragment: String): Unit = js("window.history.replaceState(null, '', '#' + fragment)")
