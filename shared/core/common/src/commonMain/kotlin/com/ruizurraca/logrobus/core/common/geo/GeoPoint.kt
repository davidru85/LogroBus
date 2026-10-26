package com.ruizurraca.logrobus.core.common.geo

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** The mean Earth radius (IUGG), in metres. */
internal const val EARTH_RADIUS_METERS = 6_371_008.8

/** A WGS84 position in decimal degrees. Never send one to analytics or logs (NFR-PRIV-02). */
data class GeoPoint(
    val lat: Double,
    val lng: Double,
)

/**
 * The great-circle (Haversine) distance to [other], in metres. Within 0.5 % of the ellipsoidal distance, which is
 * enough for "nearby" lists and walking estimates.
 */
fun GeoPoint.distanceMetersTo(other: GeoPoint): Double {
    val lat1 = lat.toRadians()
    val lat2 = other.lat.toRadians()
    val halfDLat = (lat2 - lat1) / 2
    val halfDLng = (other.lng - lng).toRadians() / 2
    val h = sin(halfDLat) * sin(halfDLat) + cos(lat1) * cos(lat2) * sin(halfDLng) * sin(halfDLng)
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(h))
}

private const val DEGREES_PER_HALF_TURN = 180.0

internal fun Double.toRadians(): Double = this * PI / DEGREES_PER_HALF_TURN
