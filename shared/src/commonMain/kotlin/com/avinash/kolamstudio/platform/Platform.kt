package com.avinash.kolamstudio.platform

expect fun readUrlFragment(): String?

expect fun writeUrlFragment(fragment: String)

expect suspend fun savePng(
    bytes: ByteArray,
    name: String,
): Boolean

expect val canShareImages: Boolean

expect suspend fun sharePng(
    bytes: ByteArray,
    name: String,
)
