package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.common.color.onColorFor
import com.ruizurraca.logrobus.transit.domain.LineKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** LB-101: the sample lines of previews and debug builds have the network's shape, and its shared colours (DD-08). */
class SampleLinesTest {
    private val byCode = SampleLines.all.associateBy { it.code }

    @Test
    fun lb101_theSampleHasTheNetworksCodes_withNoLine8() {
        assertEquals(
            setOf("1", "2", "3", "4", "5", "6", "7", "9", "10", "11", "B1", "B2", "B3"),
            byCode.keys,
        )
        assertEquals(SampleLines.all, SampleLines.all.distinctBy { it.id })
    }

    @Test
    fun lb101_nightLinesAreTheBLines() {
        SampleLines.all.forEach { line ->
            assertEquals(if (line.code.startsWith("B")) LineKind.NIGHT else LineKind.DAY, line.kind, line.code)
        }
    }

    @Test
    fun dd08_lines4AndB1ShareAColour_asDo9AndB2() {
        assertEquals(byCode.getValue("4").color, byCode.getValue("B1").color)
        assertEquals(byCode.getValue("9").color, byCode.getValue("B2").color)
    }

    @Test
    fun lb101_eachBadgeTextContrastsWithItsColour() {
        SampleLines.all.forEach { assertEquals(onColorFor(it.color), it.onColor, it.code) }
    }

    @Test
    fun lb101_oneSampleLineIsNotOperational_soItsStatusChipShows() {
        assertTrue(SampleLines.all.count { !it.isOperational } == 1)
    }
}
