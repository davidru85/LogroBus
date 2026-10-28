package com.ruizurraca.logrobus.core.common.color

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Opaque black, as an ARGB value. */
internal const val BLACK: Long = 0xFF000000

/** Opaque white, as an ARGB value. */
internal const val WHITE: Long = 0xFFFFFFFF

/** WCAG 2.2's contrast ratio between two opaque ARGB colours, from 1 (none) to 21 (black on white). */
fun contrastRatio(
    first: Long,
    second: Long,
): Double {
    val lighter = max(luminance(first), luminance(second))
    val darker = min(luminance(first), luminance(second))
    return (lighter + FLARE) / (darker + FLARE)
}

/**
 * NFR-A11Y-06: the text colour for [background], an opaque ARGB line colour. Black or white, whichever contrasts
 * more, so a line badge's code stays readable whatever colour the source sends.
 */
fun onColorFor(background: Long): Long =
    if (contrastRatio(background, WHITE) > contrastRatio(background, BLACK)) WHITE else BLACK

// WCAG 2.2 relative luminance: each sRGB channel linearised, then weighted.
@Suppress("MagicNumber") // The constants of the WCAG and sRGB definitions.
private fun luminance(argb: Long): Double {
    fun channel(shift: Int): Double {
        val value = ((argb shr shift) and 0xFF) / 255.0
        return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
}

// WCAG's allowance for ambient light, added to both luminances.
private const val FLARE = 0.05
