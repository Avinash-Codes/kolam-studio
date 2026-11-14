@file:OptIn(ExperimentalWasmJsInterop::class)

package com.avinash.kolamstudio.platform

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.download
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js

internal actual fun readShareLink(fragment: String): String? = locationWithoutHash() + "#" + fragment

internal actual fun readUrlFragment(): String? = locationHash().removePrefix("#").ifEmpty { null }

internal actual fun writeUrlFragment(fragment: String) = replaceLocationHash(fragment)

internal actual suspend fun savePng(
    bytes: ByteArray,
    name: String,
): Boolean {
    FileKit.download(bytes, fileName = "$name.png")
    return true
}

internal actual val canShareImages: Boolean = false

internal actual suspend fun sharePng(
    bytes: ByteArray,
    name: String,
) = Unit

private fun locationWithoutHash(): String = js("window.location.href.split('#')[0]")

private fun locationHash(): String = js("window.location.hash")

private fun replaceLocationHash(fragment: String): Unit = js("window.history.replaceState(null, '', '#' + fragment)")
