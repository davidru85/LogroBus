package com.ruizurraca.logrobus.feature.nearme.domain

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.geo.BoundingBox
import com.ruizurraca.logrobus.core.common.geo.GeoPoint
import com.ruizurraca.logrobus.transit.domain.NearbyStopRow
import com.ruizurraca.logrobus.transit.domain.NearbyStopsSource
import com.ruizurraca.logrobus.transit.domain.WalkDistance
import com.ruizurraca.logrobus.transit.domain.WalkDistances
import com.ruizurraca.logrobus.transit.domain.WalkDistancesRequest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * LB-203 (FR-HOME-03, FR-STOP-11, api_spec.md §4.8): the stops a rider can walk to. The 800 m radius, the 5 shown, the
 * straight-line order until API-08 answers, its walking order after, the 40 candidates the request may carry, and the
 * 50 m the rider must move before it is asked again.
 */
class DefaultGetNearbyStopsTest {
    private val here = GeoPoint(42.4650, -2.4450)

    @Test
    fun lb203_onlyStopsWithinEightHundredMetres_areListed() =
        runTest {
            val stops = listOf(stop(1, 42.4650), stop(2, 42.4730), stop(3, 42.4658))

            val listed = stopsFor(stops).stops

            // 0.008° of latitude is 890 m, past the radius, and 0.0008° is 89 m: the far one is out, the near ones in.
            // Stop 1 sits on the rider's own position, so it comes first.
            assertEquals(listOf(1, 3), listed.map { it.stopId }, "nearest first, and the far stop left out")
        }

    @Test
    fun lb203_atMostFiveStops_areShown() =
        runTest {
            val stops = (1..8).map { stop(it, 42.4650 + it * 0.0002) }

            val listed = stopsFor(stops).stops

            assertEquals(5, listed.size)
            assertEquals(listOf(1, 2, 3, 4, 5), listed.map { it.stopId })
        }

    @Test
    fun lb203_theRequestCarriesOnlyTheFortyNearestCandidates() =
        runTest {
            val stops = (1..60).map { stop(it, 42.4650 + it * 0.0001) }
            val api = RecordingWalkDistances()

            stopsFor(stops, api)

            val sent = api.requests.single().destinations
            assertEquals(40, sent.size, "api_spec.md §4.8 caps the request at 40")
            assertEquals((1..40).toList(), sent.mapNotNull { it.id }, "the nearest forty, in order")
        }

    @Test
    fun lb203_walkingDistancesReSortTheList_andFillTheWalk() =
        runTest {
            // The nearest in a straight line is the furthest on foot, which is what API-08 is for.
            val stops = listOf(stop(1, 42.4652), stop(2, 42.4658), stop(3, 42.4700))
            val api = RecordingWalkDistances(
                distances = mapOf(1 to (700 to 540), 2 to (150 to 120), 3 to (300 to 240)),
            )

            val listed = stopsFor(stops, api)

            assertTrue(listed.isRefined, "the list says its distances are walking ones")
            assertEquals(listOf(2, 3, 1), listed.stops.map { it.stopId })
            assertEquals(listOf(150, 300, 700), listed.stops.map { it.distanceMeters })
            assertEquals(120_000L, listed.stops.single { it.stopId == 2 }.walkingDuration?.inWholeMilliseconds)
        }

    @Test
    fun lb203_theListIsTheStraightLineUntilTheWalkAnswers() =
        runTest {
            val stops = listOf(stop(1, 42.4652), stop(2, 42.4658))

            val listed = stopsFor(stops, api = null)

            assertFalse(listed.isRefined)
            assertEquals(listOf(1, 2), listed.stops.map { it.stopId })
            assertTrue(listed.stops.all { it.walkingDuration == null }, "a straight line says nothing about the walk")
        }

    @Test
    fun lb203_aFailedWalkKeepsTheStraightLineOrder_ratherThanEmptyingTheSection() =
        runTest {
            val stops = listOf(stop(1, 42.4652), stop(2, 42.4658))
            val api = FailingWalkDistances()

            val listed = stopsFor(stops, api)

            assertFalse(listed.isRefined)
            assertEquals(listOf(1, 2), listed.stops.map { it.stopId })
        }

    @Test
    fun lb203_aRiderWhoBarelyMoved_doesNotAskApi08Again() =
        runTest {
            // api_spec.md §4.8: asked again only past 50 m. 0.0002° of latitude is 22 m, 0.0006° is 67 m.
            val api = RecordingWalkDistances(distances = mapOf(1 to (100 to 80)))
            val source = sourceOf(listOf(stop(1, 42.4650)))

            DefaultGetNearbyStops(source, api)(
                flowOf(here, GeoPoint(42.4652, -2.4450), GeoPoint(42.4654, -2.4450)),
            ).last()

            assertEquals(1, api.requests.size, "two positions inside 50 m share the first answer")
        }

    @Test
    fun lb203_aRiderWhoMovedEnough_asksApi08Again() =
        runTest {
            val api = RecordingWalkDistances(distances = mapOf(1 to (100 to 80)))
            val source = sourceOf(listOf(stop(1, 42.4650)))

            DefaultGetNearbyStops(source, api)(flowOf(here, GeoPoint(42.4657, -2.4450))).last()

            assertEquals(2, api.requests.size, "0.0007° of latitude is 78 m, past the threshold")
        }

    /** The stops the use case gives, from one position, with [api] for the walking distances. */
    private suspend fun stopsFor(
        stops: List<NearbyStopRow>,
        api: WalkDistances? = RecordingWalkDistances(),
    ): NearbyStops {
        val source =
            object : NearbyStopsSource {
                override fun stopsIn(box: BoundingBox) = AppResult.Success(stops)
            }
        return DefaultGetNearbyStops(source, api)(flowOf(here)).last()
    }

    /** The catalogue as the test controls it: the stops it holds, whatever box is asked for. */
    private fun sourceOf(stops: List<NearbyStopRow>) =
        object : NearbyStopsSource {
            override fun stopsIn(box: BoundingBox) = AppResult.Success(stops)
        }

    /** API-08 as the test controls it: the distances it answers, and the requests it was asked. */
    private class RecordingWalkDistances(
        private val distances: Map<Int, Pair<Int, Int>> = emptyMap(),
    ) : WalkDistances {
        val requests = mutableListOf<WalkDistancesRequest>()

        override suspend fun walkDistances(request: WalkDistancesRequest): AppResult<List<WalkDistance>> {
            requests += request
            return AppResult.Success(
                distances.map { (id, metersAndSeconds) ->
                    WalkDistance(
                        stopId = id,
                        distanceMeters = metersAndSeconds.first,
                        durationSeconds = metersAndSeconds.second,
                    )
                },
            )
        }
    }

    /** API-08 failing, as an unreachable source does. */
    private class FailingWalkDistances : WalkDistances {
        override suspend fun walkDistances(request: WalkDistancesRequest): AppResult<List<WalkDistance>> =
            AppResult.Failure(AppError.Network)
    }

    private fun stop(
        id: Int,
        lat: Double,
    ) = NearbyStopRow(stopId = id, name = "Stop $id", lat = lat, lng = -2.4450)
}
