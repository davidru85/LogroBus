package com.ruizurraca.logrobus.core.database

import com.ruizurraca.logrobus.core.common.text.searchWords

/**
 * A stop's `normalized_name`: the [searchWords] of its [name], one space apart. FTS4's simple tokenizer then sees the
 * same words that [searchStops] queries, since it splits only on ASCII characters that are not letters or digits.
 */
fun searchableName(name: String): String = searchWords(name).joinToString(" ")

/** No stop has this number, so the search matches by name only. */
private const val NO_STOP = -1L

/**
 * FR-STOP-01: the stops whose name has a word that starts with each word of [text], ignoring accents, `ñ` and case
 * (NFR-I18N-03). When [text] is a number, the stop with that number comes first. The words reach the FTS query as
 * prefixes of the folded name only, so quotes, minus, star, colons and OR in [text] are never search syntax.
 */
fun LogroBusDatabase.searchStops(text: String): List<Stop> {
    val words = searchWords(text)
    if (words.isEmpty()) return emptyList()
    val match = words.joinToString(" ") { "normalized_name:$it*" }
    return stopQueries.search(id = text.trim().toLongOrNull() ?: NO_STOP, match = match).executeAsList()
}
