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
    /**
     * FR-LINE-03, LB-102 part 2: the names of the stops the line's patterns pass, one per name and already folded as
     * the stop search folds them ([com.ruizurraca.logrobus.core.common.text.searchWords]), so a query matches a line by
     * a stop it serves ("Iglesia de Lardero" finds 1 and B3). Empty for a line with no patterns.
     */
    val stopNames: List<String> = emptyList(),
    /**
     * FR-LINE-04, LB-104: each direction the line runs with its stops in sequence order, for the detail's direction
     * toggle and ordered list. Keyed by the directions present; a line with no patterns has none.
     */
    val patterns: Map<Direction, LinePattern> = emptyMap(),
)

/** One direction of a line as the detail shows it (FR-LINE-04, LB-104). */
data class LinePattern(
    /** The last stop's name, which the toggle reads as "Towards <headsign>" (spec §5.1). */
    val headsign: String,
    /** In sequence order: a stop's position in the list is its order on the route. */
    val stops: List<LineStop>,
)

/** A stop of a pattern: who it is and where it is (FR-LINE-04). */
data class LineStop(
    val stopId: Int,
    val name: String,
    val lat: Double,
    val lng: Double,
)
