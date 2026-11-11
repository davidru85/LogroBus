package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.warn
import com.ruizurraca.logrobus.core.network.IdParser
import com.ruizurraca.logrobus.core.network.trimmedOrNull
import com.ruizurraca.logrobus.core.polling.PollKey
import com.ruizurraca.logrobus.core.polling.PollingEngine
import com.ruizurraca.logrobus.transit.data.dto.EstimatedCallDto
import com.ruizurraca.logrobus.transit.domain.Arrival
import com.ruizurraca.logrobus.transit.domain.Direction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.time.Instant

private const val TAG = "StopArrivals"

/**
 * FR-STOP-03, FR-STOP-04 and FR-STOP-05: the calls at one stop, polled while its detail shows them, over API-06.
 *
 * Every live request goes through [PollingEngine] (AGENTS.md §5.7), so two screens watching the same stop share one
 * request and the polling stops when the flow stops being collected. [interval] and [previewMinutes] are constructor
 * values: the composition root passes `poll_arrivals_ms` and the 60 min FR-STOP-03 asks for, so this class holds no
 * policy of its own.
 *
 * Unlike API-05, the calls at a stop belong to more than one line, so a collector asks for the lines the stop serves.
 * The response carries the instant it was fetched, which is what "Updated N s ago" measures (FR-ERR-06): the screen
 * never reads the device clock for that.
 */
internal class StopArrivalsRepositoryImpl(
    private val api: RealtimeApi,
    private val polling: PollingEngine,
    private val interval: Duration,
    private val previewMinutes: Int,
    private val logger: Logger,
    /** DD-04: where each line's patterns stop, so a call's opaque `directionRef` can be placed. */
    private val patternsOf: (lineId: Int) -> Map<Direction, List<Int>> = { emptyMap() },
) : StopArrivalsReader {
    override fun observeStopArrivals(
        stopId: Int,
        lineIds: List<Int>,
    ): Flow<AppResult<StopCallsAnswer>> =
        polling
            .poll(key = PollKey("stop-arrivals:$stopId:${lineIds.joinToString(",")}"), interval = interval) {
                api.arrivals(stopId = stopId, lineIds = lineIds, previewMinutes = previewMinutes)
            }.map { timed ->
                val lastError = timed.lastError
                // A failure after a success is the result until a call succeeds: a list of ETAs from the last poll
                // re-worded as "Now" is worse than none (FR-ERR-05).
                if (lastError != null) {
                    AppResult.Failure(lastError)
                } else {
                    when (val decoded = timed.value) {
                        null -> {
                            AppResult.Failure(AppError.Data)
                        }

                        else -> {
                            val calls = decoded.items
                            val inferred = directionsOf(calls)
                            val arrivals = calls.mapNotNull { call -> arrivalOf(call, inferred) }
                            AppResult.Success(
                                StopCallsAnswer(
                                    arrivals = arrivals.sortedBy { it.expectedAt },
                                    fetchedAt = timed.fetchedAt,
                                ),
                            )
                        }
                    }
                }
            }

    /**
     * DD-04: the direction of each `directionRef` in this response, inferred once over the calls that carry it, against
     * the patterns of the line it belongs to. A code the inference leaves undecided keeps a null direction.
     */
    private fun directionsOf(calls: List<EstimatedCallDto>): Map<String, Direction> {
        val byLineAndCode = calls.groupBy { call -> IdParser.lineId(call.lineRef) to call.directionRef.trimmedOrNull() }
        val inferred = mutableMapOf<String, Direction>()
        for ((key, carrying) in byLineAndCode) {
            val (lineId, code) = key
            if (lineId == null || code == null) continue
            val placed =
                carrying.mapNotNull { call ->
                    val stop = IdParser.stopId(call.stopPointRef)
                    val order = call.order
                    if (stop == null || order == null) null else EtCall(stop, order)
                }
            EtDirectionInferrer.infer(placed, patternsOf(lineId))?.let { inferred[code] = it.direction }
        }
        return inferred
    }

    /**
     * One call, or `null` when the source's code, line, stop or time cannot be read. A dropped call is logged by cause
     * only, never with the payload (NFR-PRIV-02). DD-07: an empty time is the source's "no value", so the expected time
     * falls back to the aimed one.
     */
    private fun arrivalOf(
        call: EstimatedCallDto,
        inferred: Map<String, Direction>,
    ): Arrival? {
        val line = IdParser.lineId(call.lineRef)
        val stop = IdParser.stopId(call.stopPointRef)
        val expected = expectedTime(call)
        if (line == null || stop == null || expected == null) {
            val cause =
                when {
                    line == null -> "its line"
                    stop == null -> "its stop"
                    else -> "its time"
                }
            logger.warn(TAG, "Dropped a call without $cause")
            return null
        }
        return Arrival(
            lineId = line,
            stopId = stop,
            direction = DirectionMapper.fromToken(call.directionRef) ?: inferred[call.directionRef.trimmedOrNull()],
            expectedAt = expected,
            isCancelled = call.cancellation == true,
            isLowConfidence = call.predictionInaccurate == true,
        )
    }

    private fun expectedTime(call: EstimatedCallDto): Instant? =
        (call.expectedArrivalTime.trimmedOrNull() ?: call.aimedArrivalTime.trimmedOrNull())?.let { raw ->
            try {
                Instant.parse(raw)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
}

/**
 * FR-STOP-03 to FR-STOP-05: the calls at a stop, polled while its detail shows them. An interface, because the
 * composition root adapts it to the feature's own answer and Swift tests replace it (architecture.md §4.6).
 */
fun interface StopArrivalsReader {
    fun observeStopArrivals(
        stopId: Int,
        lineIds: List<Int>,
    ): Flow<AppResult<StopCallsAnswer>>
}

/**
 * One answer of API-06: the calls at the stop, and the instant the source's answer arrived. FR-ERR-06's "Updated N s
 * ago" is measured from [fetchedAt], which the polling engine stamps from the corrected clock, so a device whose clock
 * is wrong still says how old its data is.
 */
data class StopCallsAnswer(
    val arrivals: List<Arrival>,
    val fetchedAt: Instant?,
)
