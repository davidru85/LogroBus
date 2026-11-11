package com.ruizurraca.logrobus.transit.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * LB-205 (FR-STOP-03, DD-16): the stop and the line that serves it, as the detail's header reads them. A value's shape
 * is what its consumers rely on, so it is pinned here rather than only through the screen (NFR-MNT-01).
 */
class StopTest {
    @Test
    fun lb205_aStopWithoutServedLinesKeepsNone() {
        val stop = Stop(id = 38, name = "Varea", lat = 42.46, lng = -2.44, isOperational = true, isProvisional = false)

        assertEquals(emptyList(), stop.servedLines, "a stop no line reaches keeps none, and the screen says so")
    }

    @Test
    fun lb205_aServedLineCarriesItsOwnPatternAndColours() {
        val line =
            ServedLine(
                lineId = 33,
                code = "33",
                headsign = null,
                direction = Direction.INBOUND,
                color = 0xFF1E7A3FL,
                onColor = 0xFFFFFFFFL,
            )

        assertEquals(Direction.INBOUND, line.direction, "DD-16: an arrival takes the headsign of its own journey")
        assertNull(line.headsign, "a pattern the source gave no headsign for keeps none rather than a guess")
        assertEquals(0xFF1E7A3FL, line.color, "DD-08: the badge's colour comes from the catalogue")
    }

    @Test
    fun lb205_aProvisionalStopSaysSo() {
        // DD-10: a stop synthesised from a pattern entry is marked, and its name is the pattern's.
        val stop =
            Stop(
                id = 383,
                name = "Provisional Templarios",
                lat = 42.46,
                lng = -2.44,
                isOperational = true,
                isProvisional = true,
            )

        assertEquals(true, stop.isProvisional)
    }
}
