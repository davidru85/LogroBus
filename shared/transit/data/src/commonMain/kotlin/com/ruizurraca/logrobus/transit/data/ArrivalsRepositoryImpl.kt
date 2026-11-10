package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.warn
import com.ruizurraca.logrobus.core.network.IdParser
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
 * class holds no policy of its own: the composition root passes `poll_arrivals_ms` and `eta_preview_minutes`.
 */
internal class ArrivalsRepositoryImpl(
    private val api: RealtimeApi,
    private val polling: PollingEngine,
    private val interval: Duration,
    private val previewMinutes: Int,
    private val logger: Logger,
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
                    // The latest failure is the result until a call succeeds: a screen shows the error with Retry
                    // rather than a list it cannot trust (FR-ERR-05). `lastError` alone would be a stale-value rule,
                    // which the arrival list does not have.
                    null -> {
                        AppResult.Failure(timed.lastError ?: AppError.Data)
                    }

                    else -> {
                        val calls = decoded.items.mapNotNull { call -> arrivalOf(call) }
                        AppResult.Success(
                            calls.filter { direction == null || it.direction == direction }.sortedBy { it.expectedAt },
                        )
                    }
                }
            }

    /**
     * One call, or `null` when the source's id, stop or time cannot be read. A dropped call is logged by type and
     * cause only: an error's message quotes the payload (NFR-PRIV-02). The expected time falls back to the aimed one,
     * which is what the row shows when the source has no prediction (spec §5.7).
     */
    private fun arrivalOf(call: EstimatedCallDto): Arrival? {
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
            direction = DirectionMapper.fromToken(call.directionRef),
            expectedAt = expected,
            isCancelled = call.cancellation == true,
            isLowConfidence = call.predictionInaccurate == true,
        )
    }

    private fun expectedTime(call: EstimatedCallDto): Instant? =
        (call.expectedArrivalTime ?: call.aimedArrivalTime)?.let { raw ->
            try {
                Instant.parse(raw)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
}
