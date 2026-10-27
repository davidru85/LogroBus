package com.ruizurraca.logrobus.core.common.text

/** Each accented Latin letter that Spanish and its neighbours use, with the letter search treats it as. */
private val FOLDED =
    mapOf(
        "aàáâãäå" to 'a',
        "cç" to 'c',
        "eèéêë" to 'e',
        "iìíîï" to 'i',
        "nñ" to 'n',
        "oòóôõö" to 'o',
        "uùúûü" to 'u',
        "yýÿ" to 'y',
    ).flatMap { (letters, base) -> letters.map { it to base } }.toMap()

/**
 * [text] as search compares it (NFR-I18N-03): lower case, with accents and the `ñ` folded to plain letters, so
 * "logrono" finds "Logroño" and "AYUNTAMIENTO" finds "Ayuntamiento". Spaces, digits and punctuation stay as they are.
 */
fun foldForSearch(text: String): String = text.lowercase().map { FOLDED[it] ?: it }.joinToString("")

/** The words of [text] as search compares them (NFR-I18N-03). */
fun searchWords(text: String): List<String> = emptyList()
