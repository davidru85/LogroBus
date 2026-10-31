package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.common.geo.BoundingBox
import com.ruizurraca.logrobus.core.common.geo.GeoPoint

/**
 * Spec §5.4, step 7: where the network runs. An item outside it, such as a vehicle at 0,0, is dropped and counted in
 * the `DataQualityReport` (LB-006 part 2).
 */
internal object ServiceArea {
    // Room for a bus between stops, and for a stop the network gains at its edge.
    private const val MARGIN_METERS = 2_000.0

    // api_spec.md API-02: the extent of the 254 stops of 2026-10-22.
    private val bounds =
        BoundingBox(south = 42.4219, west = -2.5206, north = 42.4925, east = -2.4050).expandedBy(MARGIN_METERS)

    /** Whether [lat] and [lng] are inside the service area; a missing position is not. */
    fun contains(
        lat: Double?,
        lng: Double?,
    ): Boolean = lat != null && lng != null && bounds.contains(GeoPoint(lat, lng))
}
