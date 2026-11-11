package com.ruizurraca.logrobus.transit.domain

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.geo.BoundingBox

/**
 * A stop of the catalogue as a "nearby" query returns it (FR-HOME-03, FR-STOP-11): its id, its name and where it is.
 * The distance is the caller's to work out, because the query is a cheap box and the exact distance a Haversine.
 *
 * [toString] names the stop and never the coordinates, so a logged result leaks no position (NFR-PRIV-02).
 */
data class NearbyStopRow(
    val stopId: Int,
    val name: String,
    val lat: Double,
    val lng: Double,
) {
    override fun toString(): String = "NearbyStopRow(stopId=$stopId)"
}

/** One stop the client asks the walking distance to (api_spec.md §4.8): its id, which comes back unchanged. */
data class WalkDestination(
    val id: Int,
    val lat: Double,
    val lng: Double,
)

/**
 * The request of `POST /api/transit/walk-distances` (api_spec.md §4.8): the rider's position and at most 40
 * destinations, pre-filtered by straight-line distance within 800 m, nearest first.
 *
 * It carries the position, so [toString] names the stop count and never the coordinates (NFR-PRIV-02).
 */
data class WalkDistancesRequest(
    val originLat: Double,
    val originLng: Double,
    val destinations: List<WalkDestination>,
) {
    override fun toString(): String = "WalkDistancesRequest(destinations=${destinations.size})"
}

/** The walking distance and duration from the request's origin to one stop (api_spec.md §4.8). */
data class WalkDistance(
    val stopId: Int,
    val distanceMeters: Int,
    val durationSeconds: Int,
)

/**
 * API-08 (api_spec.md §4.8, FR-HOME-03, FR-STOP-11): the walking distance and duration from a position to each stop
 * near it. An interface, so a feature may ask for it without depending on the data layer (ADR-0007), and Swift reaches
 * it through a use case of its own (architecture.md §5.2).
 */
fun interface WalkDistances {
    suspend fun walkDistances(request: WalkDistancesRequest): AppResult<List<WalkDistance>>
}

/**
 * FR-HOME-03 and FR-STOP-11: the stops of the cached catalogue inside [BoundingBox], read in one query so the box does
 * the cheap filtering and the caller the exact distance. Ordered by id, so a caller's own sort is the only order that
 * matters.
 */
fun interface NearbyStopsSource {
    fun stopsIn(box: BoundingBox): AppResult<List<NearbyStopRow>>
}
