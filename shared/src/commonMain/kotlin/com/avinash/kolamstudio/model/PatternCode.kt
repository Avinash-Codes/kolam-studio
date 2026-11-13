package com.avinash.kolamstudio.model

import kotlin.io.encoding.Base64

private const val BITS_PER_STATE = 2
private const val STATES_PER_BYTE = 4
private const val HEADER_BYTES = 2

private val codec = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

fun Pattern.toCode(): String {
    val packedStates =
        states.chunked(STATES_PER_BYTE) { chunk ->
            chunk.foldIndexed(0) { index, byte, state -> byte or (state.ordinal shl (BITS_PER_STATE * index)) }.toByte()
        }
    return codec.encode(byteArrayOf(grid.rows.toByte(), grid.cols.toByte()) + packedStates)
}

fun patternFromCode(code: String): Pattern? {
    val bytes =
        try {
            codec.decode(code)
        } catch (_: IllegalArgumentException) {
            return null
        }
    if (bytes.size < HEADER_BYTES || bytes[0] < 1 || bytes[1] < 1) return null
    val grid = Grid(rows = bytes[0].toInt(), cols = bytes[1].toInt())
    if (bytes.size != HEADER_BYTES + (grid.midpointCount + STATES_PER_BYTE - 1) / STATES_PER_BYTE) return null
    val states =
        List(grid.midpointCount) { index ->
            val byte = bytes[HEADER_BYTES + index / STATES_PER_BYTE].toInt()
            val ordinal = (byte shr (BITS_PER_STATE * (index % STATES_PER_BYTE))) and 0b11
            MidpointState.entries.getOrNull(ordinal) ?: return null
        }
    return Pattern(grid, states)
}
