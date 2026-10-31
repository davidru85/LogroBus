package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.transit.domain.LineKind

/** A line's code, such as `"B3"`, the name the app shows, such as "Lardero - El Campillo", and its kind. */
internal data class LineName(
    val code: String,
    val displayName: String,
    val kind: LineKind,
)

/** Spec §5.5: API-01's `name`, such as `"B3-LARDERO-EL CAMPILLO"`, after the DD-11 overrides. */
internal object LineNameParser {
    /**
     * The parts of [rawName]. When a terminus holds a hyphen, the name cannot tell where the termini split, so the
     * names of the outbound pattern's first and last stops ([outboundTermini]) are its termini.
     */
    fun parse(
        rawName: String,
        outboundTermini: Pair<String, String>? = null,
    ): LineName = LineName(rawName, outboundTermini?.first ?: rawName, LineKind.DAY) // TDD: red
}
