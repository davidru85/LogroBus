package com.ruizurraca.logrobus.core.common.color

import kotlin.math.roundToInt

private const val RGB = 3
private const val RGBA = 4
private const val CHANNEL_MAX = 255
private const val HEX_DIGITS = 6
private const val HEX = 16

private val functionalNotation = Regex("""rgba?\(([^)]*)\)""", RegexOption.IGNORE_CASE)

/**
 * DD-08: a line colour as the source writes it, `rgba(15,220,252,1)` in API-01 or `#0FDCFC` in API-07, as an ARGB
 * value; `null` when it is missing or malformed.
 */
fun parseCssColor(raw: String?): Long? {
    val text = raw?.trim().orEmpty()
    val functional = functionalNotation.matchEntire(text)
    return if (functional != null) {
        fromChannels(functional.groupValues[1].split(',').map(String::trim))
    } else {
        fromHex(text.removePrefix("#"))
    }
}

// rgb(r,g,b) or rgba(r,g,b,a): three whole channels from 0 to 255, and an alpha from 0 to 1.
private fun fromChannels(parts: List<String>): Long? {
    val channels = parts.take(RGB).mapNotNull { it.toChannel() }
    val alpha = if (parts.size == RGBA) parts[RGB].toAlpha() else CHANNEL_MAX
    return if (parts.size in RGB..RGBA && channels.size == RGB && alpha != null) argb(alpha, channels) else null
}

// RRGGBB, always opaque.
private fun fromHex(digits: String): Long? =
    if (digits.length == HEX_DIGITS && digits.all { it.isHexDigit() }) {
        argb(CHANNEL_MAX, digits.chunked(2).map { it.toInt(HEX) })
    } else {
        null
    }

private fun String.toChannel(): Int? =
    if (isNotEmpty() && all { it in '0'..'9' }) toIntOrNull()?.takeIf { it <= CHANNEL_MAX } else null

private fun String.toAlpha(): Int? {
    val alpha = toDoubleOrNull()
    return if (alpha != null && alpha in 0.0..1.0) (alpha * CHANNEL_MAX).roundToInt() else null
}

private fun Char.isHexDigit(): Boolean = this in '0'..'9' || lowercaseChar() in 'a'..'f'

@Suppress("MagicNumber") // The bit positions of ARGB.
private fun argb(
    alpha: Int,
    rgb: List<Int>,
): Long = (alpha.toLong() shl 24) or (rgb[0].toLong() shl 16) or (rgb[1].toLong() shl 8) or rgb[2].toLong()
