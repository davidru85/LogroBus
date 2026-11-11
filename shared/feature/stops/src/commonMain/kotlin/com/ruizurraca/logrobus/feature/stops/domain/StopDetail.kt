package com.ruizurraca.logrobus.feature.stops.domain

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.transit.domain.Stop
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/**
 * One call at a stop, as the stop's detail shows it (FR-STOP-03, spec §5.7): whose badge it carries ([lineCode] on
 * [color] in [onColor], ARGB), the [headsign] of the journey it belongs to, when it is expected, and the two
 * qualifiers the row draws — [isCancelled] struck through, [isLowConfidence] with DD-09's "~" prefix.
 *
 * The badge comes from the row rather than from a lookup by line, because a stop's list mixes lines and the row must
 * render before the catalogue is read.
 */
data class StopArrivalRow(
    val lineCode: String,
    val color: Long,
    val onColor: Long,
    val headsign: String?,
    val expectedAt: Instant,
    val isCancelled: Boolean,
    val isLowConfidence: Boolean,
)

/**
 * One answer of API-06 for the stop detail (FR-STOP-03, FR-STOP-05): the [stop], with the served lines its header
 * shows, and its [arrivals] as the repository orders them, earliest first.
 *
 * [fetchedAt] is the instant the source answered, taken from the corrected clock by the polling engine, and is what
 * "Updated N s ago" measures (FR-ERR-06, spec §8.6): the screen never reads the device clock for it. It is `null`
 * until the first answer of this collection. [lastError] is the error of the latest attempt when it failed while
 * earlier data stays available (FR-ERR-03), so a failed refresh never throws away a good list.
 */
data class StopArrivals(
    val stop: Stop,
    val arrivals: List<StopArrivalRow>,
    val fetchedAt: Instant?,
    val lastError: AppError?,
)

/**
 * FR-STOP-03: the stop to show, with its served lines read from the cached catalogue, or the failure to read it. A
 * `fun interface` because Swift calls it and Swift tests replace it (architecture.md §4.6).
 */
fun interface ObserveStopDetail {
    operator fun invoke(stopId: Int): Flow<AppResult<Stop>>
}

/**
 * FR-STOP-03 to FR-STOP-05: the calls at stop [stopId], polled over API-06 while its detail shows them, or the failure
 * to get them. [lineIds] is the stop's served lines, which API-06 sends as `lines`, so the request asks for exactly the
 * lines that call there; its `previewMinutes` is FR-STOP-03's 60 (api_spec.md, API-06).
 *
 * Polling and its 20 s interval belong to the repository, so this use case holds no schedule of its own, and the
 * polling stops when the flow stops being collected.
 */
fun interface ObserveStopArrivals {
    operator fun invoke(
        stopId: Int,
        lineIds: List<Int>,
    ): Flow<AppResult<StopArrivals>>
}
