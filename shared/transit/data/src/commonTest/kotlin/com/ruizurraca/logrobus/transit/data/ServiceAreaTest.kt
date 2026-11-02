package com.ruizurraca.logrobus.transit.data

import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** api_spec.md API-02: the stops lie within lat 42.4219 to 42.4925 and lng -2.5206 to -2.4050; 2 km are added. */
class ServiceAreaTest {
    @Test
    fun lb006_aPointAmongTheStopsIsInTheServiceArea() {
        assertTrue(ServiceArea.contains(42.4650, -2.4456))
    }

    @Test
    fun lb006_aPointUpToTwoKilometresBeyondTheStopsIsInTheServiceArea() {
        assertTrue(ServiceArea.contains(NORTH + latitude(km = 1.9), MIDDLE_LNG))
        assertTrue(ServiceArea.contains(SOUTH - latitude(km = 1.9), MIDDLE_LNG))
        assertTrue(ServiceArea.contains(MIDDLE_LAT, EAST + longitude(km = 1.9)))
        assertTrue(ServiceArea.contains(MIDDLE_LAT, WEST - longitude(km = 1.9)))
    }

    @Test
    fun lb006_aPointMoreThanTwoKilometresBeyondTheStopsIsNot() {
        assertFalse(ServiceArea.contains(NORTH + latitude(km = 2.1), MIDDLE_LNG))
        assertFalse(ServiceArea.contains(SOUTH - latitude(km = 2.1), MIDDLE_LNG))
        assertFalse(ServiceArea.contains(MIDDLE_LAT, EAST + longitude(km = 2.1)))
        assertFalse(ServiceArea.contains(MIDDLE_LAT, WEST - longitude(km = 2.1)))
    }

    @Test
    fun lb006_aBrokenPositionIsNotInTheServiceArea() {
        assertFalse(ServiceArea.contains(0.0, 0.0))
        assertFalse(ServiceArea.contains(-2.4456, 42.4650))
        assertFalse(ServiceArea.contains(null, -2.4456))
        assertFalse(ServiceArea.contains(42.4650, null))
        assertFalse(ServiceArea.contains(Double.NaN, -2.4456))
    }

    private fun latitude(km: Double): Double = km / KM_PER_DEGREE

    private fun longitude(km: Double): Double = km / (KM_PER_DEGREE * cos(MIDDLE_LAT * PI / 180))

    private companion object {
        const val NORTH = 42.4925
        const val SOUTH = 42.4219
        const val EAST = -2.4050
        const val WEST = -2.5206
        const val MIDDLE_LAT = 42.46
        const val MIDDLE_LNG = -2.45

        // Along a meridian, on the mean Earth radius.
        const val KM_PER_DEGREE = 111.195
    }
}
