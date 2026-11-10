package com.ruizurraca.logrobus.feature.lines.presentation

import com.ruizurraca.logrobus.core.preferences.EtaFormat
import com.ruizurraca.logrobus.core.presentation.EtaDisplay
import com.ruizurraca.logrobus.transit.domain.Arrival
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * What one stop's row of the line detail shows (LB-105, FR-LINE-05): the next arrival's [eta], and null when the stop
 * has no call, which the row draws as the criterion's "–". [isLowConfidence] is DD-09's "~" prefix, and [isCancelled]
 * strikes the row through while keeping its time (design_guidelines.md §6.3). Each platform words the [EtaDisplay];
 * shared code never spells one out.
 */
data class StopArrivalUi(
    val eta: EtaDisplay?,
    val isLowConfidence: Boolean,
    val isCancelled: Boolean,
)

/**
 * LB-105 (FR-LINE-05, spec §5.7): the next arrival of each of [stops], keyed by the stop's id, as the [now] clock and
 * the user's ETA [format] show it. [arrivals] are the calls of the line and direction the caller asked for: filtering
 * by line or direction is the repository's, not this mapper's.
 *
 * A stop's next call is its earliest that is not cancelled and not more than a minute past, which is where spec §5.7
 * drops an arrival: exactly a minute past is still arriving. A cancelled call stands in only when the stop has no
 * live one, since the criterion strikes the row through rather than hiding it, and a stop with no call at all keeps no
 * ETA. [now] and [format] reach [EtaDisplay.of] only.
 */
fun arrivalsByStop(
    stops: List<StopRowUi>,
    arrivals: List<Arrival>,
    now: Instant,
    format: EtaFormat,
): Map<Int, StopArrivalUi> {
    val callsByStop = arrivals.filter { it.expectedAt >= now - 1.minutes }.groupBy { it.stopId }
    return stops.associate { stop ->
        val calls = callsByStop[stop.stopId].orEmpty()
        val next =
            calls.filterNot { it.isCancelled }.minByOrNull { it.expectedAt } ?: calls.minByOrNull { it.expectedAt }
        stop.stopId to
            StopArrivalUi(
                eta = next?.let { EtaDisplay.of(it.expectedAt, now, format) },
                isLowConfidence = next?.isLowConfidence == true,
                isCancelled = next?.isCancelled == true,
            )
    }
}
