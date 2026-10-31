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
}
