package com.ruizurraca.logrobus.core.common.geo

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max

private const val MAX_LATITUDE = 90.0

/**
 * A latitude and longitude rectangle in decimal degrees, used to query nearby stops cheaply before an exact
 * distance check. It does not cross the antimeridian, which no Logroño query needs.
 */
data class BoundingBox(
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double,
) {
    fun contains(point: GeoPoint): Boolean = point.lat in south..north && point.lng in west..east

    /**
     * This box with every edge moved [meters] outwards. The longitude margin is computed at the edge farthest from
     * the equator, where a degree of longitude is shortest, so the margin is at least [meters] at every latitude.
     */
    fun expandedBy(meters: Double): BoundingBox {
        val dLat = metersToDegreesOfLatitude(meters)
        val newSouth = (south - dLat).coerceAtLeast(-MAX_LATITUDE)
        val newNorth = (north + dLat).coerceAtMost(MAX_LATITUDE)
        val farthestFromEquator = max(abs(newSouth), abs(newNorth))
        val dLng = dLat / cos(farthestFromEquator.toRadians())
        return BoundingBox(south = newSouth, west = west - dLng, north = newNorth, east = east + dLng)
    }

    companion object {
        /** The smallest box that holds every point within [radiusMeters] of [center]. */
        fun around(
            center: GeoPoint,
            radiusMeters: Double,
        ): BoundingBox = BoundingBox(center.lat, center.lng, center.lat, center.lng).expandedBy(radiusMeters)
    }
}

private fun metersToDegreesOfLatitude(meters: Double): Double = meters / EARTH_RADIUS_METERS / 1.0.toRadians()
