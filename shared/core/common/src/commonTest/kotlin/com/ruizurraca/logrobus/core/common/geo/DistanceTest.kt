package com.ruizurraca.logrobus.core.common.geo

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** LB-004: the Haversine distance is within 0.5 % of the geodesic distance on the WGS84 ellipsoid. */
class DistanceTest {
    // References: Vincenty's inverse formula on WGS84, computed when this test was written.
    private val acrossLogrono = Triple(GeoPoint(42.4627, -2.4449), GeoPoint(42.4500, -2.4200), 2_487.0)
    private val logronoToMadrid = Triple(GeoPoint(42.4650, -2.4456), GeoPoint(40.4169, -3.7035), 250_576.2)

    private fun assertWithinHalfAPercent(
        expectedMeters: Double,
        actualMeters: Double,
    ) {
        val error = abs(actualMeters - expectedMeters) / expectedMeters
        assertTrue(error < 0.005, "$actualMeters m is ${error * 100} % away from $expectedMeters m")
    }

    @Test
    fun lb004_haversineDistanceIsWithinHalfAPercentOfTheReference() {
        listOf(acrossLogrono, logronoToMadrid).forEach { (from, to, reference) ->
            assertWithinHalfAPercent(reference, from.distanceMetersTo(to))
        }
    }

    @Test
    fun lb004_distanceIsSymmetricAndZeroToItself() {
        val (from, to, _) = acrossLogrono

        assertEquals(from.distanceMetersTo(to), to.distanceMetersTo(from))
        assertEquals(0.0, from.distanceMetersTo(from))
    }
}
