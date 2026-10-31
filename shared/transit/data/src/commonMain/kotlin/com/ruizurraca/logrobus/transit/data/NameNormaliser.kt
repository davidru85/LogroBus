package com.ruizurraca.logrobus.transit.data

/**
 * DD-11: names as the source writes them, with typos such as `PRADOVIEJOO` and trailing spaces. The fixes come from
 * remote config's `name_overrides` ([overrides], typo to fix), never from code.
 */
internal class NameNormaliser(
    private val overrides: Map<String, String>,
) {
    /** [raw] trimmed, with every override applied; `null` when it is missing or blank. */
    fun normalise(raw: String?): String? = raw?.takeIf { overrides.isNotEmpty() } ?: raw // TDD: red
}
