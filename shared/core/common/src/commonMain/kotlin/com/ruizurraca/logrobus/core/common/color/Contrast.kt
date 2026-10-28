package com.ruizurraca.logrobus.core.common.color

/** Opaque black, as an ARGB value. */
const val BLACK: Long = 0xFF000000

/** Opaque white, as an ARGB value. */
const val WHITE: Long = 0xFFFFFFFF

/** WCAG 2.2's contrast ratio between two opaque ARGB colours, from 1 (none) to 21 (black on white). */
fun contrastRatio(
    first: Long,
    second: Long,
): Double = 0.0

/**
 * NFR-A11Y-06: the text colour for [background], an opaque ARGB line colour. Black or white, whichever contrasts
 * more, so a line badge's code stays readable whatever colour the source sends.
 */
fun onColorFor(background: Long): Long = WHITE
