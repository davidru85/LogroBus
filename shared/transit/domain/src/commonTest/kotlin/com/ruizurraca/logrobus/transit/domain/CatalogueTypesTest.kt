package com.ruizurraca.logrobus.transit.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/** Spec §5.1: the directions of a pattern and the kinds of line. */
class CatalogueTypesTest {
    @Test
    fun lb008_aPatternRunsOutboundOrInbound() {
        assertEquals(listOf(Direction.OUTBOUND, Direction.INBOUND), Direction.entries)
    }

    @Test
    fun lb008_aLineRunsByDayOrAtNight() {
        assertEquals(listOf(LineKind.DAY, LineKind.NIGHT), LineKind.entries)
    }

    @Test
    fun lb104_aLineCarriesItsPatternsAndEachPatternItsStops() {
        // FR-LINE-04: the detail's toggle and ordered list read these, so their shape is the story's contract.
        val stop = LineStop(stopId = 38, name = "Iglesia de Lardero", lat = 42.426317, lng = -2.461335)
        val pattern = LinePattern(headsign = "Iglesia de Lardero", stops = listOf(stop))
        val line =
            Line(
                id = 1,
                code = "1",
                displayName = "Lardero - Hospital San Pedro",
                color = 0xFF269905,
                onColor = 0xFF000000,
                kind = LineKind.DAY,
                isOperational = true,
                patterns = mapOf(Direction.OUTBOUND to pattern),
            )

        assertEquals("Iglesia de Lardero", line.patterns.getValue(Direction.OUTBOUND).headsign)
        assertEquals(listOf(stop), line.patterns.getValue(Direction.OUTBOUND).stops)
        assertEquals(
            38,
            line.patterns
                .getValue(Direction.OUTBOUND)
                .stops
                .single()
                .stopId,
        )
        // A line with no patterns has none, rather than an empty pattern nobody asked for.
        assertEquals(emptyMap(), line.copy(patterns = emptyMap()).patterns)
    }
}
