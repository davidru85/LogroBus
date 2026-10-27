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

/** Decomposed text (NFD) writes an accent as its own character after the letter, such as U+0303 in "ñ". */
private val COMBINING_MARKS = '\u0300'..'\u036F'

/**
 * [text] as search compares it (NFR-I18N-03): lower case, with accents and the `ñ` folded to plain letters, so
 * "logrono" finds "Logroño" and "AYUNTAMIENTO" finds "Ayuntamiento". Spaces, digits and punctuation stay as they are.
 */
fun foldForSearch(text: String): String =
    text
        .lowercase()
        .filterNot { it in COMBINING_MARKS }
        .map { FOLDED[it] ?: it }
        .joinToString("")

/**
 * The words of [text] as search compares them (NFR-I18N-03): the runs of letters and digits of [foldForSearch]. Any
 * other character separates words, such as a typographic apostrophe, a non-breaking space or a dash.
 */
fun searchWords(text: String): List<String> =
    foldForSearch(text)
        .map { if (it.isLetterOrDigit()) it else ' ' }
        .joinToString("")
        .split(' ')
        .filter(String::isNotEmpty)
