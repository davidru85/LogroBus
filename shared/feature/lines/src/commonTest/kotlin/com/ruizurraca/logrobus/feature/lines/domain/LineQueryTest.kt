package com.ruizurraca.logrobus.feature.lines.domain

import com.ruizurraca.logrobus.transit.domain.Line
import com.ruizurraca.logrobus.transit.domain.LineKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * LB-102 (FR-LINE-03, NFR-I18N-03): which lines a search finds by their code, their name, their headsigns or the name
 * of a stop their patterns pass, ignoring case and accents. The stop names arrive folded from the repository, so this
 * test hands them folded too, as `Line.stopNames` holds them.
 */
class LineQueryTest {
    private val lines =
        listOf(
            line(1, "1", "Lardero - Hospital San Pedro"),
            line(2, "2", "Yagüe - Varea"),
            line(4, "4", "Pradoviejo - Cascajos"),
            line(9, "9", "Puente Madre - Pradoviejo"),
            line(10, "10", "El Arco - Hospital San Pedro"),
            line(11, "11", "Artesanos - Manuel de Falla"),
            line(101, "B1", "Varea - La Cava"),
        )

    private fun found(query: String): List<Int> = lines.filter(LineQuery(query)::matches).map { it.id }

    @Test
    fun lb102_aTerminusFindsEveryLineThatEndsThere() {
        assertEquals(listOf(4, 9), found("pradoviejo"))
    }

    @Test
    fun lb102_aQueryWithoutItsAccentsOrInCapitalsStillMatches() {
        assertEquals(listOf(2), found("yague"))
        assertEquals(listOf(2), found("YAGÜE"))
    }

    @Test
    fun lb102_aHeadsignNotInTheNameMatches() {
        val line = line(3, "3", "Campillo - El Cubo", headsigns = listOf("Estación de Autobuses", "Campillo"))

        assertTrue(LineQuery("estacion").matches(line))
    }

    @Test
    fun lb102_aCodeMatchesFromItsStart() {
        assertEquals(listOf(1, 10, 11), found("1"))
        assertEquals(listOf(10), found("10"))
        assertEquals(listOf(101), found("b1"))
    }

    @Test
    fun lb102_eachWordMustBeginAWordOfTheLine() {
        assertEquals(listOf(1), found("hosp lard"))
        assertEquals(emptyList(), found("dero"))
    }

    @Test
    fun lb102_aCodeTypedWithASpaceOrADashStillMatches() {
        // "B 1" splits into two words, and no word of line B1 begins with "1".
        assertEquals(listOf(101), found("B 1"))
        assertEquals(listOf(101), found("b-1"))
    }

    @Test
    fun lb102_theWordsOfAQueryMayMatchDifferentFields() {
        // A code and a terminus, or a headsign and a code: each word may begin a word of any field.
        val line = line(3, "3", "Campillo - El Cubo", headsigns = listOf("Estación de Autobuses", "Campillo"))

        assertEquals(listOf(10), found("10 arco"))
        assertEquals(listOf(101), found("cava b1"))
        assertTrue(LineQuery("3 estacion").matches(line))
    }

    @Test
    fun lb102_aQueryWithoutTheEnyeMatchesANameWithIt() {
        val line = line(12, "12", "Logroño - La Grajera")

        assertTrue(LineQuery("logrono").matches(line))
        assertTrue(LineQuery("LOGROÑO").matches(line))
    }

    @Test
    fun lb102_aQueryWithNoLettersOrDigitsIsNotASearch() {
        assertFalse(LineQuery("").isActive)
        assertFalse(LineQuery("  - ").isActive)
        assertTrue(LineQuery(" b1 ").isActive)
    }

    @Test
    fun lb102_aStopNameFindsALineWhosePatternsPassIt() {
        val withStops =
            listOf(
                line(1, "1", "Lardero - Hospital San Pedro", stopNames = listOf("iglesia de lardero")),
                line(2, "2", "Yagüe - Varea", stopNames = listOf("iglesia de varea")),
                line(101, "B1", "Varea - La Cava", stopNames = listOf("iglesia de varea", "lardero")),
            )

        // Each word of the query must begin a word of a stop the line passes: 1 passes "Iglesia de Lardero", and B1
        // passes a stop called "Lardero", which is a different stop with the same word.
        assertEquals(
            listOf(1, 101),
            withStops.filter(LineQuery("lardero")::matches).map { it.id },
        )
        assertEquals(
            listOf(2, 101),
            withStops.filter(LineQuery("iglesia varea")::matches).map { it.id },
        )
    }

    @Test
    fun lb102_aStopNameIsNotAFreeTextSearch_everyWordMustBeginOne() {
        val line = line(1, "1", "Lardero - Hospital San Pedro", stopNames = listOf("iglesia de lardero"))

        // A query's word must begin a word of the stop's name, never merely sit inside it: "iglesia" matches, and
        // "iglesias" does not, because the stored word is shorter than the query's. A word no stop name has fails too.
        assertTrue(LineQuery("iglesia").matches(line))
        assertTrue(LineQuery("igles").matches(line))
        assertFalse(LineQuery("iglesias").matches(line))
        assertFalse(LineQuery("tranvia").matches(line))
    }

    private fun line(
        id: Int,
        code: String,
        displayName: String,
        headsigns: List<String> = emptyList(),
        stopNames: List<String> = emptyList(),
    ) = Line(
        id = id,
        code = code,
        displayName = displayName,
        color = 0xFF0FDCFC,
        onColor = 0xFF000000,
        kind = if (code.startsWith("B")) LineKind.NIGHT else LineKind.DAY,
        isOperational = true,
        headsigns = headsigns,
        stopNames = stopNames,
    )
}
