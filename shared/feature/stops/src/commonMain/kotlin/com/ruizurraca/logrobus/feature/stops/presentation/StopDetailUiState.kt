package com.ruizurraca.logrobus.feature.stops.presentation

import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.core.presentation.EtaDisplay
import com.ruizurraca.logrobus.transit.domain.Stop

/**
 * A stop's detail (SC-05, FR-STOP-03 to FR-STOP-05), in the UX states of spec §7.3: the stop's header (its name and the
 * badges of the lines that serve it), the arrival list in spec §5.7's display rules, and how old the list is.
 *
 * The same state serves both doors to the screen (FR-NAV-07): pushed from line detail, or in Near Me's sheet
 * (FR-NEAR-04, LB-203). Grouping and the line filter are LB-207's and are not here yet.
 */
sealed interface StopDetailUiState {
    data object Loading : StopDetailUiState

    data class Content(
        /** The stop with its [Stop.servedLines], as the cached catalogue has it (FR-STOP-03, DD-16). */
        val stop: Stop,
        /** Spec §5.7: chronological by default, with arrivals more than a minute past already dropped (FR-STOP-04). */
        val arrivals: List<StopArrivalUi>,
        /** FR-ERR-06: how old the list is, or `null` until the first answer of this collection arrives. */
        val updated: StopUpdatedUi?,
    ) : StopDetailUiState

    /** FR-ERR-05: the message for [reason], and Retry. */
    data class Error(
        val reason: ErrorReason,
    ) : StopDetailUiState
}

/**
 * One arrival of the stop detail (FR-STOP-04, spec §5.7): the badge of its line ([lineCode] on [color] in [onColor],
 * ARGB), the [headsign] of the journey it belongs to (DD-16, `null` when unknown, DD-04), its [eta], and the two
 * qualifiers the row draws — [isCancelled] struck through with the label "Cancelled", [isLowConfidence] DD-09's "~".
 *
 * [eta] is always filled by [StopDetailUiMapper.rows]; it is nullable so a platform can carry a row with no ETA of its
 * own. Each platform words the [EtaDisplay]; shared code never spells one out (AGENTS.md §5, rule 4).
 */
data class StopArrivalUi(
    val lineCode: String,
    val color: Long,
    val onColor: Long,
    val headsign: String?,
    val eta: EtaDisplay?,
    val isCancelled: Boolean,
    val isLowConfidence: Boolean,
)

/**
 * How old the arrival list is (FR-STOP-05, spec §5.7, design_guidelines.md §7): [ageSeconds] words "Updated N s ago",
 * [isStale] from 50 s puts it in the warning colour, and [needsRefresh] past 120 s means every ETA is already a clock
 * time and the screen offers "Tap to refresh".
 *
 * Measured from the answer's own instant against the graph's `AppClock` (`ServerCorrectedClock`), never the platform
 * clock (FR-ERR-06, spec §8.6).
 */
data class StopUpdatedUi(
    /** Whole seconds from the answer to `now`, floored: what `freshness.updated_ago` interpolates. */
    val ageSeconds: Int,
    val isStale: Boolean,
    val needsRefresh: Boolean,
)
