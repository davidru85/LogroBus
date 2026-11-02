package com.ruizurraca.logrobus.transit.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Spec §5.1: a line is its code and the rest of its data, never its colour alone. DD-08's handling is tested in
 * LinesUiMapperTest.
 */
class LineTest {
    private val line4 = Line(4, "4", "Casa Blanca - Yagüe", 0xFFE30613, 0xFFFFFFFF, LineKind.DAY, isOperational = true)

    @Test
    fun lb101_linesThatShareAColourAreStillTwoLines() {
        val lineB1 = line4.copy(id = 101, code = "B1", displayName = "Varea - La Cava", kind = LineKind.NIGHT)

        assertEquals(line4.color, lineB1.color)
        assertNotEquals(line4, lineB1)
    }

    @Test
    fun lb101_aLineThatStopsRunningIsNoLongerTheSameLine() {
        // FR-LINE-02: a sync that changes the status changes what the list shows, so the list sees a new line.
        assertNotEquals(line4, line4.copy(isOperational = false))
        assertEquals(line4, line4.copy())
    }
}
