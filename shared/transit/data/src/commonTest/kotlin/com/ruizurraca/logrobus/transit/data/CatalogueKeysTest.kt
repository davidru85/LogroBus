package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.transit.domain.Direction
import com.ruizurraca.logrobus.transit.domain.LineKind
import kotlin.test.Test
import kotlin.test.assertEquals

/** The keys the static sync stores for directions and line kinds, on devices, so they never change. */
class CatalogueKeysTest {
    @Test
    fun lb008_directionsAreStoredUnderKeysThatNeverChange() {
        assertEquals(listOf("outbound", "inbound"), Direction.entries.map { it.key })
    }

    @Test
    fun lb008_lineKindsAreStoredUnderKeysThatNeverChange() {
        assertEquals(listOf("day", "night"), LineKind.entries.map { it.key })
    }
}
