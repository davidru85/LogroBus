package com.ruizurraca.logrobus.core.common.geo

import kotlin.test.Test
import kotlin.test.assertFalse

/** NFR-PRIV-02: coordinates never reach logs, so geo values never print them, even through string templates. */
class GeoPrivacyTest {
    @Test
    fun lb004_geoPointsAndBoundingBoxesDoNotPrintTheirCoordinates() {
        val point = GeoPoint(42.4650, -2.4456)
        val box = BoundingBox.around(point, radiusMeters = 2_000.0)

        listOf("$point", "$box", "${listOf(point)}").forEach { printed ->
            assertFalse(printed.contains("42.4") || printed.contains("2.4"), printed)
        }
    }
}
