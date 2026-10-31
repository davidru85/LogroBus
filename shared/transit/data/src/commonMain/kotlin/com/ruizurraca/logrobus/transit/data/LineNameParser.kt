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
    // Spec §5.5: these stay lower case, except as a name's first word.
    private val linkWords = setOf("de", "del", "la", "las", "el", "los", "y")

    /**
     * The parts of [rawName]. When a terminus holds a hyphen, the name cannot tell where the termini split, so the
     * names of the outbound pattern's first and last stops ([outboundTermini]) are its termini.
     */
    fun parse(
        rawName: String,
        outboundTermini: Pair<String, String>? = null,
    ): LineName {
        val code = rawName.substringBefore('-').trim()
        val parts =
            rawName
                .substringAfter('-', missingDelimiterValue = "")
                .split('-')
                .map(String::trim)
                .filter(String::isNotEmpty)
        val termini = if (parts.size == 2) parts[0] to parts[1] else outboundTermini
        val displayName =
            termini?.let { (from, to) -> "${titleCase(from)} - ${titleCase(to)}" }
                ?: parts.joinToString(" - ", transform = ::titleCase).ifEmpty { code }
        // Spec §5.1: the night (Búho) lines' codes start with B.
        val kind = if (code.startsWith('B', ignoreCase = true)) LineKind.NIGHT else LineKind.DAY
        return LineName(code, displayName, kind)
    }

    private fun titleCase(text: String): String =
        text
            .lowercase()
            .split(' ')
            .filter(String::isNotEmpty)
            .mapIndexed { index, word -> if (index > 0 && word in linkWords) word else word.capitalised() }
            .joinToString(" ")

    private fun String.capitalised(): String = replaceFirstChar { it.titlecase() }
}
