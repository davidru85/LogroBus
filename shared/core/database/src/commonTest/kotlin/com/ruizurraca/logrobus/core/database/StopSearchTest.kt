package com.ruizurraca.logrobus.core.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTimedValue

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
        // By searchable name: "gran via 38" before "gran via ayuntamiento".
        assertEquals(listOf(2L, 1L), search("via gran"))
        assertEquals(listOf(1L), search("vía ayuntam"))
    }

    @Test
    fun lb007_theIndexAndTheQuerySplitWordsAlike() {
        stops(
            1L to "Hermanos D\u2019Elhuyar",
            2L to "Logron\u0303o Deporte",
            3L to "Gran\u00A0Vía",
            4L to "Deportivo\u2013Hospital",
        )

        assertEquals(listOf(1L), search("elhuyar"))
        assertEquals(listOf(2L), search("logrono"))
        assertEquals(listOf(3L), search("via"))
        assertEquals(listOf(4L), search("hospital"))
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

    @Test
    fun lb007_theIndexFollowsRenamedAndDeletedStops() {
        stops(1L to "Gran Vía", 2L to "Vara de Rey")

        // As a sync would: plain SQL on the stop table only.
        driver.execute(null, "UPDATE stop SET name = 'Mercado', normalized_name = 'mercado' WHERE id = 1", 0)
        driver.execute(null, "DELETE FROM stop WHERE id = 2", 0)
        stops(3L to "Vara de Rey 2")

        assertEquals(emptyList<Long>(), search("gran"))
        assertEquals(listOf(1L), search("mercado"))
        assertEquals(listOf(3L), search("vara"))
    }

    @Test
    fun lb007_theIndexFollowsAStopReplacedInPlace() {
        stops(1L to "Gran Vía")

        // INSERT OR REPLACE is SQLite 3.18's only upsert; it deletes the old row without firing delete triggers.
        driver.execute(
            null,
            "INSERT OR REPLACE INTO stop (id, name, lat, lng, operational, provisional, normalized_name) " +
                "VALUES (1, 'Mercado', 42.4650, -2.4456, 1, 0, 'mercado')",
            0,
        )

        assertEquals(emptyList<Long>(), search("gran"))
        assertEquals(listOf(1L), search("mercado"))
    }

    @Test
    fun lb007_searchingTheWholeNetworkTakesUnder100Ms() {
        // NFR-PERF-03, on the JVM and the iOS simulator: 254 stops, the size of the network. The criterion's
        // reference devices are measured with the app (LB-007 in acceptance_criteria.md).
        val streets =
            listOf("Gran Vía", "Avda. de Portugal", "Logroño Deporte", "Vara de Rey", "Pío XII", "Ayuntamiento")
        (1L..254L).forEach { id -> driver.insertStop(id, "${streets[(id % streets.size).toInt()]} $id") }
        search("vara") // The first query prepares the statement.

        val timings =
            listOf(
                "logrono",
                "AYUNTAMIENTO",
                "38",
                "gran via",
                "pio xii 2",
            ).map { measureTimedValue { search(it) } }

        assertTrue(timings.all { it.value.isNotEmpty() }, "$timings")
        assertTrue(timings.all { it.duration < 100.milliseconds }, "$timings")
    }
}
