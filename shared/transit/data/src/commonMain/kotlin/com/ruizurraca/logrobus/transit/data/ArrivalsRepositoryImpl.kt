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
import com.ruizurraca.logrobus.transit.domain.ArrivalsRepository
import com.ruizurraca.logrobus.transit.domain.Direction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.time.Instant

private const val TAG = "Arrivals"

/**
 * FR-LINE-05 and spec §5.7: the calls of one line, polled while its detail shows them, over API-05.
 *
 * Every live request goes through [PollingEngine] (AGENTS.md §5.7), so two screens watching the same line share one
 * request, the cadence comes from remote config with its 10 s floor, and the polling stops when the flow stops being
 * collected. [interval] and [previewMinutes] are constructor parameters rather than reads of remote config, so this
 * class holds no policy of its own: the composition root passes `poll_arrivals_ms` and the line's own 30 min window
 * (spec §5.7, §8.7). `eta_preview_minutes` is FR-STOP-03's 60 min for API-06, not this one's.
 */
internal class ArrivalsRepositoryImpl(
    private val api: RealtimeApi,
    private val polling: PollingEngine,
    private val interval: Duration,
    private val previewMinutes: Int,
    private val logger: Logger,
    /**
     * DD-04 and spec §5.6: where each of the line's patterns stops, the outbound one first. ET's `directionRef` is an
     * opaque code, so a call's direction is inferred from where it is in the journey; without the patterns, no call
     * can be attributed, and asking for a direction would empty every row.
     */
    private val patternsOf: (lineId: Int) -> Map<Direction, List<Int>> = { emptyMap() },
) : ArrivalsRepository {
    override fun observeLineArrivals(
        lineId: Int,
        direction: Direction?,
    ): Flow<AppResult<List<Arrival>>> =
        polling
            .poll(key = PollKey("arrivals:$lineId:${direction?.key ?: "any"}"), interval = interval) {
                api.arrivals(lineId, previewMinutes)
            }.map { timed ->
                when (val decoded = timed.value) {
                    // The latest failure is the result until a call succeeds. The engine keeps the last good value
                    // beside the error, and a list of ETAs from 20 s ago re-worded as "Now" is worse than none: a
                    // screen drops the calls to the dash and keeps Retry for the line (FR-ERR-05).
                    null -> {
                        AppResult.Failure(timed.lastError ?: AppError.Data)
                    }

                    else -> {
                        val lastError = timed.lastError
                        if (lastError != null) return@map AppResult.Failure(lastError)
                        val inferred = directionsOf(decoded.items, patternsOf(lineId))
                        val calls = decoded.items.mapNotNull { call -> arrivalOf(call, inferred) }
                        AppResult.Success(
                            calls.filter { direction == null || it.direction == direction }.sortedBy { it.expectedAt },
                        )
                    }
                }
            }

    /**
     * DD-04: the direction of each `directionRef` in this response, inferred once from the whole response, since the
     * shares that decide it are computed over the calls that carry that code together. A code the inference leaves
     * undecided keeps a null direction, so its calls are never put on a pattern.
     */
    private fun directionsOf(
        calls: List<EstimatedCallDto>,
        patterns: Map<Direction, List<Int>>,
    ): Map<String, Direction> =
        calls
            .groupBy { it.directionRef.trimmedOrNull() }
            .mapNotNull { (code, carrying) ->
                if (code == null) return@mapNotNull null
                val placed =
                    carrying.mapNotNull { call ->
                        val stop = IdParser.stopId(call.stopPointRef)
                        val order = call.order
                        if (stop == null || order == null) null else EtCall(stop, order)
                    }
                EtDirectionInferrer.infer(placed, patterns)?.let { code to it.direction }
            }.toMap()

    /**
     * One call, or `null` when the source's id, stop or time cannot be read. A dropped call is logged by type and
     * cause only: an error's message quotes the payload (NFR-PRIV-02). The expected time falls back to the aimed one,
     * which is what the row shows when the source has no prediction (spec §5.7).
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
            // DD-04: the source's own word when it gives one, the inference otherwise.
            direction = DirectionMapper.fromToken(call.directionRef) ?: inferred[call.directionRef.trimmedOrNull()],
            expectedAt = expected,
            isCancelled = call.cancellation == true,
            isLowConfidence = call.predictionInaccurate == true,
        )
    }

    // DD-07: the source writes "" for a time it does not have, so an empty string falls through to the aimed one
    // rather than failing to parse and dropping the whole call.
    private fun expectedTime(call: EstimatedCallDto): Instant? =
        (call.expectedArrivalTime.trimmedOrNull() ?: call.aimedArrivalTime.trimmedOrNull())?.let { raw ->
            try {
                Instant.parse(raw)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
}
