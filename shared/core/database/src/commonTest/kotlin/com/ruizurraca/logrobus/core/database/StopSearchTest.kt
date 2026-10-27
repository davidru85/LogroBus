package com.ruizurraca.logrobus.core.database

import kotlin.test.Test
import kotlin.test.assertEquals

/** FR-STOP-01 and NFR-I18N-03: stop search ignores accents, `ñ` and case, and finds a stop by its number. */
class StopSearchTest {
    private val driver = testDriver()
    private val database = LogroBusDatabase(driver)

    private fun stops(vararg stops: Pair<Long, String>) = stops.forEach { (id, name) -> driver.insertStop(id, name) }

    private fun search(text: String) = database.searchStops(text).map { it.id }

    @Test
    fun lb007_searchIgnoresAccentsEnyeAndCase() {
        stops(1L to "Logroño Deporte", 2L to "Ayuntamiento", 3L to "Avda. de Portugal")

        assertEquals(listOf(1L), search("logrono"))
        assertEquals(listOf(2L), search("AYUNTAMIENTO"))
        assertEquals(listOf(3L), search("AVDA DE PORTUGAL"))
    }

    @Test
    fun lb007_searchMatchesTheStartOfEveryWordInAnyOrder() {
        stops(1L to "Gran Vía - Ayuntamiento", 2L to "Gran Vía 38", 3L to "Vara de Rey")

        assertEquals(listOf(1L), search("ayunt"))
        assertEquals(listOf(1L, 2L), search("via gran"))
        assertEquals(listOf(1L), search("vía ayuntam"))
    }

    @Test
    fun lb007_aNumberFindsTheStopWithThatIdFirst() {
        stops(5L to "Gran Vía 38", 38L to "Vara de Rey", 380L to "Piqueras")

        // Stop 38 by its number, then the names with a word that starts with 38. Stop 380 is not a match.
        assertEquals(listOf(38L, 5L), search("38"))
    }

    @Test
    fun lb007_searchTextIsNeverReadAsSearchSyntax() {
        stops(1L to "Gran Vía", 2L to "Vara de Rey")

        // Quotes, minus, star, colon and OR are syntax in an FTS query; here they are only separators or words.
        assertEquals(listOf(1L), search("\"gran\" -vía*"))
        assertEquals(listOf(1L), search("gran:vía"))
        assertEquals(emptyList<Long>(), search("gran OR vara"))
        assertEquals(emptyList<Long>(), search(" ¿?¡! "))
        assertEquals(emptyList<Long>(), search(""))
    }
}
