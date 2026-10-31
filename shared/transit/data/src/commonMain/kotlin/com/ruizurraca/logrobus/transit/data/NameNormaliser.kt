package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.network.trimmedOrNull

/**
 * DD-11: names as the source writes them, with typos such as `PRADOVIEJOO` and trailing spaces. The fixes come from
 * remote config's `name_overrides` ([overrides], typo to fix), never from code.
 */
internal class NameNormaliser(
    overrides: Map<String, String>,
) {
    private val fixes = overrides.filterKeys { it.isNotBlank() }

    // Every typo in one pass, longest first: a short typo never breaks a longer one that contains it, and no fix is
    // rewritten by another override. A typo matches as whole words, in its exact case, never inside a longer word:
    // letters, digits and combining accents (NFD) all belong to a word.
    private val typos =
        fixes.keys
            .sortedByDescending { it.length }
            .takeIf { it.isNotEmpty() }
            ?.joinToString("|", prefix = """(?<![\p{L}\p{M}\p{N}])(?:""", postfix = """)(?![\p{L}\p{M}\p{N}])""") {
                Regex.escape(it)
            }?.let(::Regex)

    /** [raw] trimmed, with every override applied; `null` when it is missing or blank. */
    fun normalise(raw: String?): String? {
        val name = raw.trimmedOrNull() ?: return null
        return (typos?.replace(name) { fixes.getValue(it.value) } ?: name).trimmedOrNull()
    }
}
