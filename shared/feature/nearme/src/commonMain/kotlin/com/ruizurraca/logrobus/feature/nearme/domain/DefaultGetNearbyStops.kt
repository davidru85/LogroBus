package com.ruizurraca.logrobus.feature.nearme.domain

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.geo.BoundingBox
import com.ruizurraca.logrobus.core.common.geo.GeoPoint
import com.ruizurraca.logrobus.core.common.geo.distanceMetersTo
import com.ruizurraca.logrobus.transit.domain.NearbyStopRow
import com.ruizurraca.logrobus.transit.domain.NearbyStopsSource
import com.ruizurraca.logrobus.transit.domain.WalkDestination
import com.ruizurraca.logrobus.transit.domain.WalkDistances
import com.ruizurraca.logrobus.transit.domain.WalkDistancesRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Duration.Companion.seconds

/** FR-STOP-11: the radius the list looks in. */
internal const val NEARBY_RADIUS_METERS = 800.0

/** FR-STOP-11: how many stops the section shows. */
internal const val NEARBY_SHOWN = 5

/** api_spec.md §4.8: the request carries at most this many candidates, the nearest first. */
internal const val NEARBY_CANDIDATES = 40

/** api_spec.md §4.8: API-08 is asked again only once the rider has moved more than this far. */
internal const val NEARBY_MOVE_METERS = 50.0

/**
 * FR-HOME-03 and FR-STOP-11: the stops a rider can walk to from where they are, nearest first.
 *
 * The list is the straight-line distance the moment it is read — the cached catalogue answers in milliseconds — and
 * becomes the walking distance once API-08 answers, which is what the specification asks a list to sort by. API-08 is
 * asked for the 40 nearest candidates only (api_spec.md §4.8), and never again until the rider has moved more than
 * 50 m, so a stationary rider causes one request.
 *
 * A failed API-08 keeps the straight-line order rather than emptying the section: the rider still sees the stops and
 * can open them, and the next position that moved enough asks again.
 */
class DefaultGetNearbyStops(
    private val stops: NearbyStopsSource,
    private val walkDistances: WalkDistances?,
    private val moveThresholdMeters: Double = NEARBY_MOVE_METERS,
) : GetNearbyStops {
    override fun invoke(position: Flow<GeoPoint>): Flow<NearbyStops> =
        position
            .distinctUntilChanged()
            .let { positions ->
                flow {
                    var lastAsked: GeoPoint? = null
                    var lastAnswer: List<NearbyStop> = emptyList()
                    var refined = false
                    positions.collect { here ->
                        val candidates = candidatesFrom(here)
                        val askedEnough =
                            lastAsked != null && lastAsked!!.distanceMetersTo(here) <= moveThresholdMeters
                        if (refined && askedEnough) {
                            // The rider has barely moved: the walking distances already in hand still apply.
                            emit(NearbyStops(stops = lastAnswer, isRefined = true))
                            return@collect
                        }
                        val straightLine = candidates.map { it.toNearby(here) }.take(NEARBY_SHOWN)
                        if (walkDistances == null) {
                            lastAnswer = straightLine
                            refined = false
                            emit(NearbyStops(stops = straightLine, isRefined = false))
                            return@collect
                        }
                        // The straight line first, so the section shows at once and never waits on the source.
                        emit(NearbyStops(stops = straightLine, isRefined = false))
                        lastAsked = here
                        when (val walked = walkDistances.walkDistances(candidates.toRequest(here))) {
                            is AppResult.Failure -> {
                                lastAnswer = straightLine
                                refined = false
                                emit(NearbyStops(stops = straightLine, isRefined = false))
                            }

                            is AppResult.Success -> {
                                val byStop = walked.value.associateBy { it.stopId }
                                // A stop API-08 left out keeps the straight-line distance it already had, so a partial
                                // answer never hides a stop the rider can see from the corner (api_spec.md §3).
                                val refinedStops =
                                    candidates
                                        .mapNotNull { candidate ->
                                            val walk = byStop[candidate.stopId] ?: return@mapNotNull candidate.toRefined(here)
                                            NearbyStop(
                                                stopId = candidate.stopId,
                                                name = candidate.name,
                                                distanceMeters = walk.distanceMeters,
                                                walkingDuration = walk.durationSeconds.seconds,
                                            )
                                        }.sortedBy { it.distanceMeters }
                                        .take(NEARBY_SHOWN)
                                lastAnswer = refinedStops
                                refined = true
                                emit(NearbyStops(stops = refinedStops, isRefined = true))
                            }
                        }
                    }
                }
            }
            .let { it }

    /** The stops within the radius, nearest by straight line, cut to the candidates the request may carry. */
    private fun candidatesFrom(here: GeoPoint): List<NearbyStopRow> {
        val box = BoundingBox.around(here, NEARBY_RADIUS_METERS)
        val rows =
            when (val read = stops.stopsIn(box)) {
                is AppResult.Failure -> return emptyList()
                is AppResult.Success -> read.value
            }
        return rows
            .filter { GeoPoint(it.lat, it.lng).distanceMetersTo(here) <= NEARBY_RADIUS_METERS }
            .sortedBy { GeoPoint(it.lat, it.lng).distanceMetersTo(here) }
            .take(NEARBY_CANDIDATES)
    }

    private fun NearbyStopRow.toNearby(here: GeoPoint) =
        NearbyStop(
            stopId = stopId,
            name = name,
            distanceMeters = distanceTo(here),
        )

    /** The straight line, kept by a stop API-08 did not answer for. */
    private fun NearbyStopRow.toRefined(here: GeoPoint) = toNearby(here)

    /** The straight-line distance in whole metres, which the list shows until the walk is known. */
    private fun NearbyStopRow.distanceTo(here: GeoPoint): Int = GeoPoint(lat, lng).distanceMetersTo(here).toInt()

    private fun List<NearbyStopRow>.toRequest(here: GeoPoint) =
        WalkDistancesRequest(
            originLat = here.lat,
            originLng = here.lng,
            destinations = map { WalkDestination(id = it.stopId, lat = it.lat, lng = it.lng) },
        )
}
