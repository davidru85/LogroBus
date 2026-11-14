package com.ruizurraca.logrobus.feature.stops.presentation

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.preferences.EtaFormat
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.core.presentation.EtaDisplay
import com.ruizurraca.logrobus.core.presentation.ServedArrivalUi
import com.ruizurraca.logrobus.feature.stops.domain.StopArrivalRow
import com.ruizurraca.logrobus.feature.stops.domain.StopArrivals
import com.ruizurraca.logrobus.transit.domain.LineKind
import com.ruizurraca.logrobus.transit.domain.Stop
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Spec §5.7: an arrival more than this far in the past is removed from the list. Exactly this far is still arriving,
 * since the rule is "more than".
 */
private val DROP_AFTER = 60.seconds

/** LB-205 (FR-STOP-05, design_guidelines.md §7): from here the list says "Updated N s ago" in the warning colour. */
private val STALE_AFTER = 50.seconds

/**
 * Spec §5.7: over this age the data is expired, so every ETA is a clock time and the screen offers "Tap to refresh".
 */
private val REFRESH_AFTER = 120.seconds

/** FR-STOP-03 to FR-STOP-05: the stop detail's state, from one answer of API-06. Pure, so both platforms agree. */
object StopDetailUiMapper {
    /**
     * FR-STOP-03: the stop's header, its arrival list and how old that list is, or the reason there is nothing to show.
     * A failure is the Error state with an [ErrorReason] (FR-ERR-05), even when earlier data was on screen: a stale
     * list re-worded as "Now" would be worse than none.
     *
     * A success for another stop is the Data error: only a stale id can ask for it, since the detail opens from a stop
     * that is shown, and showing another stop's calls under this one's name would be a lie.
     *
     * [now] is the graph's `AppClock` (`ServerCorrectedClock`), never the platform clock (FR-ERR-06, spec §8.6); every
     * ETA and the age of the list are measured against it. Once the list is past 120 s, the rows come as clock times
     * whatever [format] is: FR-STOP-05 and LB-205's criterion say "Given more than 120 s, then ETAs switch to clock
     * times with 'Tap to refresh'", so overriding the user's [EtaFormat.MINUTES] there is the rule itself and not a
     * bug. [format] is the user's ETA format otherwise (FR-SET-01).
     */
    fun state(
        result: AppResult<StopArrivals>,
        stopId: Int,
        now: Instant,
        format: EtaFormat,
    ): StopDetailUiState =
        when (result) {
            is AppResult.Success -> {
                val answer = result.value
                if (answer.stop.id != stopId) {
                    StopDetailUiState.Error(ErrorReason.DATA)
                } else {
                    val updated = updatedOf(answer.fetchedAt, now)
                    // Spec §5.7: an expired list replaces every ETA with a clock time, whatever the user's format is.
                    val shownFormat = if (updated?.needsRefresh == true) EtaFormat.CLOCK_TIME else format
                    val rows = rows(answer.arrivals, now, shownFormat)
                    StopDetailUiState.Content(
                        stop = answer.stop,
                        arrivals = rows,
                        updated = updated,
                        empty = emptyOf(answer.stop, rows),
                    )
                }
            }

            is AppResult.Failure -> {
                StopDetailUiState.Error(ErrorReason.of(result.error))
            }
        }

    /**
     * FR-STOP-06: the message an empty list shows, or `null` while there are rows. A stop is night-only when every line
     * that serves it is a Búho line ([LineKind.of]); the message is shared, its night sentence is the platform's.
     */
    private fun emptyOf(
        stop: Stop,
        rows: List<ServedArrivalUi>,
    ): StopDetailEmptyUi? =
        when {
            rows.isNotEmpty() -> {
                null
            }

            stop.servedLines.isNotEmpty() && stop.servedLines.all { LineKind.of(it.code) == LineKind.NIGHT } -> {
                StopDetailEmptyUi.NO_BUSES_THEN_NIGHT
            }

            else -> {
                StopDetailEmptyUi.NO_BUSES
            }
        }

    /**
     * FR-STOP-04, spec §5.7: the rows of one answer, chronological, with those more than a minute past removed. An
     * arrival in 30 s is "Now", in 4 min "4 min", in 75 min a clock time. The rule itself is [EtaDisplay.of]'s, which
     * shared code applies so that both platforms word the same case.
     */
    fun rows(
        arrivals: List<StopArrivalRow>,
        now: Instant,
        format: EtaFormat,
    ): List<ServedArrivalUi> =
        arrivals
            .filter { it.expectedAt >= now - DROP_AFTER }
            .sortedBy { it.expectedAt }
            .map { row ->
                ServedArrivalUi(
                    lineCode = row.lineCode,
                    color = row.color,
                    onColor = row.onColor,
                    headsign = row.headsign,
                    eta = EtaDisplay.of(row.expectedAt, now, format),
                    isCancelled = row.isCancelled,
                    isLowConfidence = row.isLowConfidence,
                )
            }

    /**
     * FR-STOP-05, FR-ERR-06: the age of the list, `null` until the first answer arrives. Both thresholds are inclusive
     * of nothing but their own rule: stale from 50 s, expired past 120 s, and never negative if the clock was moved
     * back between the answer and the render.
     */
    private fun updatedOf(
        fetchedAt: Instant?,
        now: Instant,
    ): StopUpdatedUi? {
        if (fetchedAt == null) return null
        val age = (now - fetchedAt).coerceAtLeast(0.seconds)
        return StopUpdatedUi(
            ageSeconds = age.inWholeSeconds.toInt(),
            isStale = age >= STALE_AFTER,
            needsRefresh = age > REFRESH_AFTER,
        )
    }
}
