package com.ruizurraca.logrobus.core.common.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** NFR-A11Y-06 and design_guidelines.md §6.2: every line badge's code reaches 4.5:1 against its line colour. */
class ContrastTest {
    // Spec §3, the line catalogue as of 2026-10: code to API-01 colour (rgba(r,g,b,1) as ARGB).
    private val currentLines =
        mapOf(
            "1" to 0xFF269905,
            "2" to 0xFFFFFF00,
            "3" to 0xFFC100FB,
            "4" to 0xFFFD0200,
            "5" to 0xFFFD82BB,
            "6" to 0xFF924C48,
            "7" to 0xFFBABABA,
            "9" to 0xFF0002FD,
            "10" to 0xFF0FDCFC,
            "11" to 0xFFFFE07D,
            "B1" to 0xFFFD0200,
            "B2" to 0xFF0002FD,
            "B3" to 0xFF01FF01,
        )

    @Test
    fun lb011_contrastRatiosFollowWcag() {
        assertEquals(21.0, contrastRatio(BLACK, WHITE), absoluteTolerance = 0.001)
        assertEquals(21.0, contrastRatio(WHITE, BLACK), absoluteTolerance = 0.001)
        assertEquals(1.0, contrastRatio(0xFF777777, 0xFF777777), absoluteTolerance = 0.001)
        // #767676 is the classic lightest grey that reaches 4.5:1 on white.
        assertEquals(4.54, contrastRatio(0xFF767676, WHITE), absoluteTolerance = 0.01)
    }

    @Test
    fun lb011_everyCurrentLineColourGetsAnOnColorOfAtLeast4point5To1() {
        val unreadable =
            currentLines.filterValues { colour -> contrastRatio(colour, onColorFor(colour)) < MINIMUM_CONTRAST }
        assertEquals(emptyMap(), unreadable)
    }

    @Test
    fun lb011_theOnColorIsWhicheverOfBlackAndWhiteContrastsMore() {
        assertEquals(WHITE, onColorFor(0xFF0002FD), "Line 9's blue")
        assertEquals(BLACK, onColorFor(0xFFFFFF00), "Line 2's yellow")
        // Line 3's purple: white just reaches 4.5:1, and black does better.
        val purple = currentLines.getValue("3")
        assertTrue(contrastRatio(purple, WHITE) >= MINIMUM_CONTRAST)
        assertEquals(BLACK, onColorFor(purple))
    }

    private companion object {
        const val MINIMUM_CONTRAST = 4.5
    }
}
