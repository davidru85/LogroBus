package com.ruizurraca.logrobus.feature.stops.domain

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.transit.domain.Arrival
import com.ruizurraca.logrobus.transit.domain.Direction
import com.ruizurraca.logrobus.transit.domain.ServedLine
import com.ruizurraca.logrobus.transit.domain.Stop
import com.ruizurraca.logrobus.transit.domain.StopRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * LB-205 (FR-STOP-03, FR-STOP-04, DD-16): the stop detail's calls come from API-06 for the lines the stop serves, and
 * each row takes the badge and headsign of its own line and direction.
 */
class DefaultObserveStopArrivalsTest {
    private val now = Instant.parse("2026-11-08T10:00:00Z")

    // Stop 38, served by line 1 both ways, as `fixtures/et_stop38.json` and the catalogue describe it.
    private val line1 =
        ServedLine(
            lineId = 1,
            code = "1",
            headsign = "Hospital San Pedro",
            direction = Direction.OUTBOUND,
            color = 0xFF269905,
            onColor = 0xFF000000,
        )
    private val line1Back = line1.copy(headsign = "Lardero", direction = Direction.INBOUND)
    private val stop38 =
        Stop(
            id = 38,
            name = "Varea",
            lat = 42.46,
            lng = -2.44,
            isOperational = true,
            isProvisional = false,
            servedLines = listOf(line1, line1Back),
        )

    @Test
    fun lb205_theCallsAreAskedForTheLinesTheStopServes() =
        runTest {
            val asked = mutableListOf<Pair<Int, List<Int>>>()
            val arrivals = useCase(asked = asked)

            arrivals(stopId = 38, lineIds = listOf(1)).first()

            assertEquals(listOf(38 to listOf(1)), asked)
        }

    @Test
    fun lb205_aRowTakesTheHeadsignOfItsOwnDirection() =
        runTest {
            val calls =
                listOf(
                    arrival(expectedAt = now + 1.minutes, direction = Direction.OUTBOUND),
                    arrival(expectedAt = now + 9.minutes, direction = Direction.INBOUND),
                )
            val result = useCase(calls = calls)(stopId = 38, lineIds = listOf(1)).first()

            val content = assertIs<AppResult.Success<StopArrivals>>(result).value
            assertEquals(
                listOf("Hospital San Pedro", "Lardero"),
                content.arrivals.map { it.headsign },
                "DD-16: the two ends of line 1 head different ways",
            )
            assertEquals(listOf(0xFF269905, 0xFF269905), content.arrivals.map { it.color })
            assertEquals(now, content.fetchedAt, "FR-ERR-06: the answer's instant is what the age measures")
        }

    @Test
    fun lb205_aStopTheCatalogueDoesNotHoldAsksTheSourceForNothing() =
        runTest {
            val asked = mutableListOf<Pair<Int, List<Int>>>()
            val arrivals = useCase(asked = asked, stop = null)

            val result = arrivals(stopId = 404, lineIds = emptyList()).first()

            assertEquals(AppResult.Failure(AppError.Data), result)
            assertEquals(emptyList(), asked, "a missing stop is not the source's error")
        }

    private fun arrival(
        expectedAt: Instant,
        direction: Direction,
    ) = Arrival(lineId = 1, stopId = 38, direction = direction, expectedAt = expectedAt)

    private fun useCase(
        asked: MutableList<Pair<Int, List<Int>>> = mutableListOf(),
        calls: List<Arrival> = emptyList(),
        stop: Stop? = stop38,
    ): DefaultObserveStopArrivals {
        val stops =
            StopRepository { stopId ->
                if (stop == null) {
                    flowOf(AppResult.Failure(AppError.Data))
                } else {
                    flowOf(AppResult.Success(stop.copy(id = stopId)))
                }
            }
        return DefaultObserveStopArrivals(stops) { stopId, lineIds ->
            asked += stopId to lineIds
            flowOf(AppResult.Success(FetchedArrivals(arrivals = calls, fetchedAt = now)))
        }
    }
}
