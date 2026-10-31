package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.network.trimmedOrNull

/**
 * DD-11: names as the source writes them, with typos such as `PRADOVIEJOO` and trailing spaces. The fixes come from
 * remote config's `name_overrides` ([overrides], typo to fix), never from code.
 */
internal class NameNormaliser(
    overrides: Map<String, String>,
) {
    // Longest first, so a short typo never breaks a longer one that contains it.
    private val rules =
        overrides.entries
            .filter { it.key.isNotBlank() }
            .sortedByDescending { it.key.length }
            .map { (typo, fix) -> wholeWords(typo) to Regex.escapeReplacement(fix) }

    /** [raw] trimmed, with every override applied; `null` when it is missing or blank. */
    fun normalise(raw: String?): String? {
        val name = raw.trimmedOrNull() ?: return null
        return rules.fold(name) { fixed, (typo, fix) -> typo.replace(fixed, fix) }.trimmedOrNull()
    }

    // A typo matches as whole words, in its exact case: never inside a longer word, accented letters included.
    private fun wholeWords(typo: String) = Regex("""(?<![\p{L}\p{N}])${Regex.escape(typo)}(?![\p{L}\p{N}])""")
}
