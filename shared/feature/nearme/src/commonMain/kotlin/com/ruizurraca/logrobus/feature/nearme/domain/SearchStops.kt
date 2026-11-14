package com.ruizurraca.logrobus.feature.nearme.domain

import com.ruizurraca.logrobus.transit.domain.ServedLine
import com.ruizurraca.logrobus.transit.domain.Stop

/**
 * FR-NEAR-05, FR-STOP-01 and FR-STOP-02: the stops a query finds, each with the lines that serve it and the headsign of
 * the journey that reaches it (DD-16), so two stops with one name are told apart.
 *
 * It answers as a list rather than a flow: a search is asked once per keystroke and its answer is a value, not a
 * collection. The database's own `searchStops` does the matching under 100 ms (NFR-PERF-03), folded for accents.
 */
fun interface SearchStops {
    suspend operator fun invoke(query: String): List<StopSearchResultUi>
}

/** One row of the stop search: the stop, and the lines that reach it, never empty for a stop the catalogue holds. */
data class StopSearchResultUi(
    val stop: Stop,
) {
    val lines: List<ServedLine> get() = stop.servedLines
}

/**
 * Searches [query] and drops a blank one before it reaches the database, where an empty FTS match would cost a read for
 * nothing. The stops it answers with are the catalogue's own rows, so their `servedLines` are already the badges and
 * headsigns the row shows.
 */
class SearchStopsOf(
    private val query: suspend (String) -> List<Stop>,
) : SearchStops {
    override suspend fun invoke(query: String): List<StopSearchResultUi> {
        if (query.isBlank()) return emptyList()
        return this.query(query.trim()).map(::StopSearchResultUi)
    }
}
