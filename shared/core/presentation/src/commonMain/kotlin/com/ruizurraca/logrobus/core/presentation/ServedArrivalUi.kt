package com.ruizurraca.logrobus.core.presentation

/**
 * One arrival, as a list of calls shows it (FR-STOP-04, spec §5.7): the badge of its line ([lineCode] on [color] in
 * [onColor], ARGB), the [headsign] of the journey it belongs to (DD-16, `null` when unknown, DD-04), its [eta], and
 * the two qualifiers a row draws — [isCancelled] struck through with "Cancelled", [isLowConfidence] DD-09's "~".
 *
 * It lives here rather than in one feature, because the stop detail and "My stops" both draw a call and neither may
 * depend on the other (architecture.md §3.2): the type is theirs to share, the wording is each platform's.
 *
 * [eta] is always filled by the mapper that builds the rows; it is nullable so a platform can carry a row with no ETA
 * of its own. Each platform words the [EtaDisplay]; shared code never spells one out (AGENTS.md §5, rule 4).
 */
data class ServedArrivalUi(
    val lineCode: String,
    val color: Long,
    val onColor: Long,
    val headsign: String?,
    val eta: EtaDisplay?,
    val isCancelled: Boolean,
    val isLowConfidence: Boolean,
)
