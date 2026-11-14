package com.avinash.kolamstudio.platform

internal expect fun readShareLink(fragment: String): String?

internal expect fun readUrlFragment(): String?

internal expect fun writeUrlFragment(fragment: String)

internal expect suspend fun savePng(
    bytes: ByteArray,
    name: String,
): Boolean

internal expect val canShareImages: Boolean

internal expect suspend fun sharePng(
    bytes: ByteArray,
    name: String,
)
