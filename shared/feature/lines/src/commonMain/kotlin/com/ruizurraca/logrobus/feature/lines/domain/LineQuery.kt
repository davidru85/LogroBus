package com.ruizurraca.logrobus.feature.lines.domain

import com.ruizurraca.logrobus.core.common.text.searchWords
import com.ruizurraca.logrobus.transit.domain.Line

/**
 * A search of the Lines tab (LB-102, FR-LINE-03, NFR-I18N-03). A line matches when each word of the query begins a
 * word of its code, its display name or its headsigns, ignoring case, accents and the `ñ`, as the stop search does
 * (`architecture.md` §4.3). Stop names come with the stops (LB-008 part 2).
 */
internal class LineQuery(
    text: String,
) {
    private val words = searchWords(text)

    /** A query with no letters or digits, such as spaces or a dash, searches nothing: the list shows as it is. */
    val isActive: Boolean get() = words.isNotEmpty()

    fun matches(line: Line): Boolean {
        val lineWords = (listOf(line.code, line.displayName) + line.headsigns).flatMap(::searchWords)
        return words.all { word -> lineWords.any { it.startsWith(word) } } || beginsTheCodeOf(line)
    }

    // A code typed with a separator, "B 1" or "B-1", is two words; together they begin "b1".
    private fun beginsTheCodeOf(line: Line): Boolean =
        searchWords(line.code).joinToString("").startsWith(words.joinToString(""))
}
