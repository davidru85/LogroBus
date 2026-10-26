package com.ruizurraca.logrobus.core.common.geo

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** LB-004: a bounding box expands correctly by 2 km (nearby stops) and by 5 km. */
class BoundingBoxTest {
    private val center = GeoPoint(42.4650, -2.4456)
    private val logrono = BoundingBox(south = 42.43, west = -2.50, north = 42.49, east = -2.40)

    private val metersPerDegreeOfLatitude = 6_371_008.8 * PI / 180

    private fun GeoPoint.north(meters: Double) = GeoPoint(lat + meters / metersPerDegreeOfLatitude, lng)

    private fun GeoPoint.east(meters: Double) =
        GeoPoint(lat, lng + meters / (metersPerDegreeOfLatitude * cos(lat * PI / 180)))

    private fun assertAbout(
        expectedMeters: Double,
        actualMeters: Double,
    ) = assertTrue(abs(actualMeters - expectedMeters) / expectedMeters < 0.005, "$actualMeters m, not $expectedMeters m")

    @Test
    fun lb004_aBoxAroundAPointReaches2KilometresInEveryDirection() {
        val box = BoundingBox.around(center, radiusMeters = 2_000.0)

        assertAbout(2_000.0, center.distanceMetersTo(GeoPoint(box.north, center.lng)))
        assertAbout(2_000.0, center.distanceMetersTo(GeoPoint(box.south, center.lng)))
        assertAbout(2_000.0, center.distanceMetersTo(GeoPoint(center.lat, box.east)))
        assertAbout(2_000.0, center.distanceMetersTo(GeoPoint(center.lat, box.west)))
    }

    @Test
    fun lb004_expandingBy5KilometresMovesEveryEdgeOutwards() {
        val expanded = logrono.expandedBy(5_000.0)

        assertAbout(5_000.0, GeoPoint(logrono.north, logrono.west).distanceMetersTo(GeoPoint(expanded.north, logrono.west)))
        assertAbout(5_000.0, GeoPoint(logrono.south, logrono.west).distanceMetersTo(GeoPoint(expanded.south, logrono.west)))
        // Longitude degrees shrink away from the equator: along the northern edge, the margin is exactly 5 km.
        assertAbout(5_000.0, GeoPoint(expanded.north, logrono.east).distanceMetersTo(GeoPoint(expanded.north, expanded.east)))
        assertAbout(5_000.0, GeoPoint(expanded.north, logrono.west).distanceMetersTo(GeoPoint(expanded.north, expanded.west)))
    }

    @Test
    fun lb004_theExpandedBoxContainsPointsWithinTheRadiusAndExcludesFartherOnes() {
        val box = BoundingBox.around(center, radiusMeters = 2_000.0)

        listOf(center.north(1_900.0), center.north(-1_900.0), center.east(1_900.0), center.east(-1_900.0))
            .forEach { assertTrue(box.contains(it), "$it should be inside") }
        listOf(center.north(2_200.0), center.north(-2_200.0), center.east(2_200.0), center.east(-2_200.0))
            .forEach { assertFalse(box.contains(it), "$it should be outside") }
    }
}
