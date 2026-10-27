package com.ruizurraca.logrobus.core.database

import com.ruizurraca.logrobus.core.common.text.foldForSearch

/** What separates the words of folded text, which leaves only letters and digits as words. */
private val NOT_A_WORD = Regex("[^a-z0-9]+")

/** A stop's `normalized_name`: its [name] as search compares it. */
fun searchableName(name: String): String = foldForSearch(name)

/** No stop has this number, so the search matches by name only. */
private const val NO_STOP = -1L

/**
 * FR-STOP-01: the stops whose name has a word that starts with each word of [text], ignoring accents, `ñ` and case
 * (NFR-I18N-03). When [text] is a number, the stop with that number comes first. The words reach the FTS query as
 * prefixes of the folded name only, so quotes, minus, star, colons and OR in [text] are never search syntax.
 */
fun LogroBusDatabase.searchStops(text: String): List<Stop> {
    val words = foldForSearch(text).split(NOT_A_WORD).filter(String::isNotEmpty)
    if (words.isEmpty()) return emptyList()
    val match = words.joinToString(" ") { "normalized_name:$it*" }
    return stopQueries.search(id = text.trim().toLongOrNull() ?: NO_STOP, match = match).executeAsList()
}
