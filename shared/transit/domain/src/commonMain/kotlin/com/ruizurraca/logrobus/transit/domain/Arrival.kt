package com.ruizurraca.logrobus.transit.domain

import com.ruizurraca.logrobus.core.common.AppResult
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/**
 * One predicted call of a line at a stop (FR-LINE-05, spec §5.7), as API-05's estimated timetable reports it.
 *
 * [direction] is `null` when the source's code says neither pattern (DD-04): the call is then never attributed to a
 * direction, and only a caller that asked for none sees it. [expectedAt] is the instant the source expects the bus,
 * never the aimed timetable: a cancelled call keeps it, because the row still says when it would have come.
 */
data class Arrival(
    val lineId: Int,
    val stopId: Int,
    val direction: Direction?,
    val expectedAt: Instant,
    val isCancelled: Boolean = false,
    /** DD-09: the source says so with `predictionInaccurate`; the row prefixes the ETA and speaks "approximately". */
    val isLowConfidence: Boolean = false,
)

/**
 * FR-LINE-05: the calls of one line, polled while the line's detail shows it. An interface, because Swift tests and
 * previews replace it (architecture.md §4.6).
 *
 * A [direction] of `null` asks for every call of the line, which is what a caller with no direction in mind needs;
 * asking for one keeps only the calls the source attributes to it, since guessing would put a bus on the wrong
 * pattern.
 */
fun interface ArrivalsRepository {
    fun observeLineArrivals(
        lineId: Int,
        direction: Direction?,
    ): Flow<AppResult<List<Arrival>>>
}
