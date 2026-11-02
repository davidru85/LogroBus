package com.ruizurraca.logrobus.transit.domain

/**
 * A line of the network (spec §5.1), as the static sync stores it, with its patterns' headsigns but not their stops or
 * shapes. The catalogue comes from the source: the code never assumes a fixed set of lines (AGENTS.md §5, rule 10).
 */
data class Line(
    val id: Int,
    /** "1", "B3" (spec §5.5). */
    val code: String,
    /** "Lardero - Hospital San Pedro", after the name overrides (DD-11). */
    val displayName: String,
    /** ARGB, from API-01 (DD-08). Lines 4 and B1, and 9 and B2, share one, so it never stands without the code. */
    val color: Long,
    /** ARGB: black or white, whichever contrasts with [color] (spec §5.1). */
    val onColor: Long,
    val kind: LineKind,
    /** FR-LINE-02: `status == 1` (OQ-02). */
    val isOperational: Boolean,
    /** FR-LINE-13: the headsign of each direction the line runs, outbound first; none for a line with no patterns. */
    val headsigns: List<String> = emptyList(),
)
