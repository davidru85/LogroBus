package com.ruizurraca.logrobus.feature.nearme.domain

import com.ruizurraca.logrobus.core.common.geo.GeoPoint
import kotlinx.coroutines.flow.Flow

/**
 * The stop Near Me lists (FR-HOME-03, FR-STOP-11): its id, its badge's line, and how far it is and how long it takes
 * to walk there.
 *
 * [distanceMeters] is the straight-line distance until API-08 answers, and the walking distance after
 * (api_spec.md §4.8). [walkingDuration] is `null` until then, because the straight line says nothing about the walk.
 * Neither carries the user's position: the list names stops, not where the rider is (NFR-PRIV-02).
 */
data class NearbyStop(
    val stopId: Int,
    val name: String,
    val distanceMeters: Int,
    val walkingDuration: kotlin.time.Duration? = null,
)

/**
 * FR-HOME-03 and FR-STOP-11: the stops a rider can walk to from where they are, nearest first. Collected with a
 * position, it starts a load and keeps it as the position changes; a collection that ends stops the work.
 *
 * It is a use case of its own rather than a repository call, so Swift reaches it through the same door as its siblings
 * (architecture.md §5.2) and a preview or a test replaces it without a catalogue or a source.
 */
fun interface GetNearbyStops {
    operator fun invoke(position: Flow<GeoPoint>): Flow<NearbyStops>
}

/**
 * What Near Me's "Near you" section shows (FR-HOME-03): the stops, and whether the walking distances have answered
 * yet. [isRefined] tells the two orders apart, so the screen can say a distance is a straight line before the walk is
 * known, and the list is re-sorted when it is.
 */
data class NearbyStops(
    val stops: List<NearbyStop>,
    val isRefined: Boolean = false,
)
