package com.ruizurraca.logrobus.feature.stops.domain

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.transit.domain.Arrival
import com.ruizurraca.logrobus.transit.domain.Stop
import com.ruizurraca.logrobus.transit.domain.StopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * FR-STOP-03, FR-STOP-04, FR-STOP-05: the stop's detail, its served lines from the catalogue and its calls from
 * API-06, as one answer for the screen.
 *
 * The stop is read first, because the calls are asked for the lines that serve it (`lines=1,33`, spec §5.7); a stop
 * the catalogue does not hold is the failure the screen shows, and nothing is asked of the source for it. Each
 * arrival takes the badge and headsign of its own line and direction from the served lines, so a row carries what it
 * draws and the screen never joins the two itself.
 */
class DefaultObserveStopArrivals(
    private val stops: StopRepository,
    private val calls: (stopId: Int, lineIds: List<Int>) -> Flow<AppResult<FetchedArrivals>>,
) : ObserveStopArrivals {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun invoke(
        stopId: Int,
        lineIds: List<Int>,
    ): Flow<AppResult<StopArrivals>> =
        stops.observeStop(stopId).distinctUntilChanged().flatMapLatest { stopResult ->
            when (stopResult) {
                is AppResult.Failure -> {
                    flowOf(stopResult)
                }

                is AppResult.Success -> {
                    val stop = stopResult.value
                    calls(stopId, lineIds).map { callsResult ->
                        when (callsResult) {
                            is AppResult.Failure -> {
                                callsResult
                            }

                            is AppResult.Success -> {
                                AppResult.Success(
                                    StopArrivals(
                                        stop = stop,
                                        arrivals = callsResult.value.arrivals.map { arrival -> rowOf(arrival, stop) },
                                        fetchedAt = callsResult.value.fetchedAt,
                                        lastError = null,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }

    /**
     * DD-16: the headsign of the journey the call belongs to. A line with two patterns heads both ways, so the call's
     * direction picks its own; a call whose direction the source leaves opaque takes the first headsign the line has,
     * since showing the other end would send a rider the wrong way.
     */
    private fun rowOf(
        arrival: Arrival,
        stop: Stop,
    ): StopArrivalRow {
        val served = stop.servedLines.firstOrNull { it.lineId == arrival.lineId }
        val sameDirection =
            served?.let { line ->
                stop.servedLines.firstOrNull {
                    it.lineId == line.lineId &&
                        it.direction == arrival.direction
                }
            }
        val line = sameDirection ?: served
        return StopArrivalRow(
            lineCode = line?.code ?: arrival.lineId.toString(),
            color = line?.color ?: 0,
            onColor = line?.onColor ?: 0,
            headsign = line?.headsign,
            expectedAt = arrival.expectedAt,
            isCancelled = arrival.isCancelled,
            isLowConfidence = arrival.isLowConfidence,
        )
    }
}

/**
 * One answer of the source for a stop's calls: the calls and the instant they arrived. [fetchedAt] is what "Updated
 * N s ago" measures (FR-ERR-06), and it comes from the polling engine's corrected clock, never the device's.
 */
data class FetchedArrivals(
    val arrivals: List<Arrival>,
    val fetchedAt: kotlin.time.Instant?,
)
