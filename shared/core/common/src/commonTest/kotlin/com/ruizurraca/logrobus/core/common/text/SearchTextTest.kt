package com.ruizurraca.logrobus.core.common.text

import kotlin.test.Test
import kotlin.test.assertEquals

/** NFR-I18N-03: search ignores accents, `ñ` and case. */
class SearchTextTest {
    @Test
    fun lb007_searchTextIgnoresAccentsEnyeAndCase() {
        val folded =
            mapOf(
                "Logroño" to "logrono",
                "AYUNTAMIENTO" to "ayuntamiento",
                "Ávila, Éibar, Íñigo, Óscar, Úbeda" to "avila, eibar, inigo, oscar, ubeda",
                "àâä èêë ìîï òôö ùûü" to "aaa eee iii ooo uuu",
                "Pingüino Ñandú Çà" to "pinguino nandu ca",
                "Avda. Pío XII, 38" to "avda. pio xii, 38",
            )

        assertEquals(folded, folded.mapValues { (text, _) -> foldForSearch(text) })
    }

    @Test
    fun lb007_searchWordsAreTheFoldedLettersAndDigitsBetweenAnythingElse() {
        // A typographic apostrophe, a combining tilde (NFD), a non-breaking space and an en dash all occur in names.
        val words =
            mapOf(
                "Hermanos D\u2019Elhuyar" to listOf("hermanos", "d", "elhuyar"),
                "Logron\u0303o" to listOf("logrono"),
                "Gran\u00A0Vía" to listOf("gran", "via"),
                "Deportivo\u2013Hospital" to listOf("deportivo", "hospital"),
                "Avda. Pío XII, 38" to listOf("avda", "pio", "xii", "38"),
                " ¿?¡! " to emptyList(),
            )

        assertEquals(words, words.mapValues { (text, _) -> searchWords(text) })
    }
}
