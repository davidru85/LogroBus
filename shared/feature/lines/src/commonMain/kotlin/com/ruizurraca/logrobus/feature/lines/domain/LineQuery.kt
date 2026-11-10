package com.ruizurraca.logrobus.feature.lines.domain

import com.ruizurraca.logrobus.core.common.text.searchWords
import com.ruizurraca.logrobus.transit.domain.Line

/**
 * A search of the Lines tab (LB-102, FR-LINE-03, NFR-I18N-03). A line matches when each word of the query begins a
 * word of its code, its display name, its headsigns or the name of a stop its patterns pass, ignoring case, accents and
 * the `ñ`, as the stop search does (`architecture.md` §4.3). The stop names arrive folded from the repository, so
 * `Line.stopNames` already holds the search's words.
 */
internal class LineQuery(
    text: String,
) {
    private val words = searchWords(text)

    /** A query with no letters or digits, such as spaces or a dash, searches nothing: the list shows as it is. */
    val isActive: Boolean get() = words.isNotEmpty()

    fun matches(line: Line): Boolean {
        val lineWords = (listOf(line.code, line.displayName) + line.headsigns).flatMap(::searchWords)
        // The stop names come folded, one word each, so they are compared as the others are, without folding again.
        val stopWords = line.stopNames.flatMap { it.split(' ') }.filter(String::isNotEmpty)
        val all = lineWords + stopWords
        return words.all { word -> all.any { it.startsWith(word) } } || beginsTheCodeOf(line)
    }

    // A code typed with a separator, "B 1" or "B-1", is two words; together they begin "b1".
    private fun beginsTheCodeOf(line: Line): Boolean =
        searchWords(line.code).joinToString("").startsWith(words.joinToString(""))
}
