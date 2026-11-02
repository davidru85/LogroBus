package com.ruizurraca.logrobus.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IdParserTest {
    @Test
    fun dd01_aStopIdInEverySourceFormatIsTheSameNumber() {
        assertEquals(152, IdParser.stopId("152"))
        assertEquals(632, IdParser.stopId("0632"))
        assertEquals(38, IdParser.stopId("38"))
        assertEquals(100, IdParser.stopId("1:100"))
    }

    @Test
    fun dd01_spacesAroundAStopIdAreIgnored() {
        assertEquals(38, IdParser.stopId(" 38 "))
    }

    @Test
    fun dd01_aMissingOrBlankStopIdIsNull() {
        assertNull(IdParser.stopId(null))
        assertNull(IdParser.stopId(""))
        assertNull(IdParser.stopId("  "))
    }

    @Test
    fun dd01_aStopIdThatIsNotAWholeNumberIsNull() {
        listOf("abc", "1:", "-5", "+5", "6.3", "1:2:3", "99999999999").forEach { raw ->
            assertNull(IdParser.stopId(raw), raw)
        }
    }

    @Test
    fun dd01_onlyTheAgencyPrefixOneIsStripped() {
        // Another prefix would be another feed's id space, which no mapper should guess at.
        assertNull(IdParser.stopId("2:100"))
    }

    @Test
    fun dd02_aLineIdInEverySourceFormatIsTheSameNumber() {
        assertEquals(33, IdParser.lineId("33"))
        assertEquals(1, IdParser.lineId("1"))
        assertEquals(5, IdParser.lineId("1:5-desc"))
        assertEquals(5, IdParser.lineId("1:5-asc"))
    }

    @Test
    fun dd02_aMissingBlankOrNonNumericLineIdIsNull() {
        listOf(null, "", " ", "B3", "1:", "1:-desc", "2:5-desc").forEach { raw ->
            assertNull(IdParser.lineId(raw), raw)
        }
    }

    @Test
    fun dd06_theLiteralObjectObjectLineRefIsNull() {
        assertNull(IdParser.lineId("[object Object]"))
    }
}
