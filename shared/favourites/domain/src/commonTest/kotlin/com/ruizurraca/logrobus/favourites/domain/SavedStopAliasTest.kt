package com.ruizurraca.logrobus.favourites.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * LB-208 (FR-STOP-08): the alias a user gives a saved stop. It is cut to the field's thirty characters, a blank one is
 * no alias at all, and the cut never splits a glyph.
 */
class SavedStopAliasTest {
    @Test
    fun lb208_anAliasIsTrimmedAndKept() {
        assertEquals("Casa", SavedStopAlias.of("  Casa  "))
    }

    @Test
    fun lb208_aBlankAliasIsNone_soTheStopKeepsItsOwnName() {
        assertNull(SavedStopAlias.of(""))
        assertNull(SavedStopAlias.of("   "))
        assertNull(SavedStopAlias.of("\t\n"))
    }

    @Test
    fun lb208_thirtyCharactersAreKept_whole() {
        val alias = "A".repeat(SavedStopAlias.MAX_LENGTH)

        assertEquals(alias, SavedStopAlias.of(alias))
    }

    @Test
    fun lb208_aLongerAliasIsCutToThirty() {
        val alias = SavedStopAlias.of("B".repeat(120))

        assertEquals(SavedStopAlias.MAX_LENGTH, alias?.length)
    }

    @Test
    fun lb208_theCutCountsCodePoints_neverSplittingAGlyph() {
        // Every glyph takes two UTF-16 units, so thirty characters is fifteen whole glyphs. A cut by units would take
        // sixty units' worth of nothing and end on half a character.
        val emoji = "\uD83D\uDE8C".repeat(40)

        val alias = SavedStopAlias.of(emoji)!!

        assertEquals(30, alias.codePointCount(0, alias.length))
        assertEquals(false, alias.last().code in 0xD800..0xDBFF, "no lone high surrogate at the cut")
    }

    @Test
    fun lb208_aGlyphAtTheBoundaryIsDroppedWhole() {
        // Thirty letters fill the field exactly, so a glyph after them does not fit: the cut ends at the letters,
        // never in the middle of the glyph.
        val alias = SavedStopAlias.of("A".repeat(30) + "\uD83D\uDE8C")

        assertEquals(30, alias?.length)
        assertEquals("A".repeat(30), alias)
        assertEquals(false, alias!!.last().code in 0xD800..0xDBFF, "no half a glyph at the cut")
    }

    @Test
    fun lb208_aSavedStopCarriesItsAliasAndItsPlace() {
        val stop = SavedStop(stopId = 38, alias = "Casa", position = 2)

        assertEquals(38, stop.stopId)
        assertEquals("Casa", stop.alias)
        assertEquals(2, stop.position)
    }
}
