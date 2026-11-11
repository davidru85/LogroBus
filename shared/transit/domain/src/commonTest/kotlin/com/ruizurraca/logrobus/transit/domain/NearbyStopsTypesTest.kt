package com.ruizurraca.logrobus.transit.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * LB-203 (NFR-PRIV-02): the nearby-stops and walking-distance types. Their `toString` names what they are and never a
 * position, so a logged result or a string template cannot leak where the rider is.
 */
class NearbyStopsTypesTest {
    @Test
    fun lb203_aNearbyStopRowNamesTheStop_neverItsPosition() {
        val row = NearbyStopRow(stopId = 38, name = "Gonzalo de Berceo", lat = 42.4652, lng = -2.4450)

        assertEquals("NearbyStopRow(stopId=38)", row.toString())
        assertFalse(row.toString().contains("42.4"), "a row's toString carries no coordinate")
    }

    @Test
    fun lb203_aWalkDistancesRequestNamesItsCount_neverTheOrigin() {
        val request =
            WalkDistancesRequest(
                originLat = 42.4650,
                originLng = -2.4450,
                destinations = listOf(WalkDestination(id = 38, lat = 42.426317, lng = -2.461335)),
            )

        assertEquals("WalkDistancesRequest(destinations=1)", request.toString())
        assertFalse(request.toString().contains("42.4"), "a request's toString carries no coordinate")
    }

    @Test
    fun lb203_aWalkDistancePairsTheStopWithItsWalk() {
        val walk = WalkDistance(stopId = 38, distanceMeters = 4906, durationSeconds = 410)

        assertEquals(38, walk.stopId)
        assertEquals(4906, walk.distanceMeters)
        assertEquals(410, walk.durationSeconds)
    }
}
