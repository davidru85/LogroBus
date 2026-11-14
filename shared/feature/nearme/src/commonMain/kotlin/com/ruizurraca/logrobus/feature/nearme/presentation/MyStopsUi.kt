package com.ruizurraca.logrobus.feature.nearme.presentation

import com.ruizurraca.logrobus.core.presentation.ServedArrivalUi
import com.ruizurraca.logrobus.favourites.domain.FAVOURITES_PER_TYPE
import com.ruizurraca.logrobus.favourites.domain.SavedStop
import com.ruizurraca.logrobus.favourites.domain.SavedStopAlias
import com.ruizurraca.logrobus.feature.nearme.domain.StopSearchResultUi

/**
 * One row of "My stops" (FR-HOME-02, FR-STOP-08): the stop, the alias it shows, its place in the user's order, and the
 * next calls there (FR-HOME-02, LB-204) — the three soonest, each worded as the stop detail words one.
 */
data class SavedStopRowUi(
    val stopId: Int,
    val label: String,
    val position: Int,
    /** FR-HOME-02, LB-204: at most [ARRIVALS_PER_ROW] calls, earliest first, cancelled ones left out. */
    val arrivals: List<ServedArrivalUi> = emptyList(),
    /** FR-ERR-03: the row's own read failed, so what it shows is what it last had. */
    val hasFailed: Boolean = false,
) {
    companion object {
        /** LB-204: how many calls a saved stop's row shows (FR-HOME-02, spec §7.6). */
        const val ARRIVALS_PER_ROW = 3
    }
}

/**
 * "My stops" (FR-HOME-02), the section above "Near you" in Near Me's sheet (FR-NEAR-03).
 *
 * [rows] is empty until the user saves a stop, and the section then reads [EMPTY_HINT] with its "Find a stop" action.
 * [limitReached] is FR-FAV-05's message, shown once and cleared, as the favourite lines' limit is.
 */
data class MyStopsUi(
    val rows: List<SavedStopRowUi> = emptyList(),
    val limitReached: Boolean = false,
) {
    val isEmpty: Boolean get() = rows.isEmpty()

    companion object {
        /** FR-STOP-08: what the section says when the user has saved nothing. */
        const val EMPTY_HINT = "Save a stop to see its next buses here"
    }
}

/** What the saved stops' screen sends its ViewModel. */
sealed interface SavedStopsEvent {
    /** FR-STOP-08: the alias the user typed, and whether the stop is saved (`false`) or removed. */
    data class SetSaved(
        val stopId: Int,
        val alias: String?,
    ) : SavedStopsEvent

    /** FR-STOP-08: a saved stop's new alias. */
    data class Rename(
        val stopId: Int,
        val alias: String?,
    ) : SavedStopsEvent

    data class Remove(
        val stopId: Int,
    ) : SavedStopsEvent

    /** FR-FAV-02: the user's new order, top to bottom. */
    data class Reorder(
        val stopIds: List<Int>,
    ) : SavedStopsEvent

    /** FR-FAV-05: the limit message was shown. */
    data object LimitShown : SavedStopsEvent
}

object SavedStopsUiMapper {
    /**
     * FR-STOP-08: the rows, showing each stop's alias when it has one. A stop the catalogue no longer holds still has
     * its row, labelled "[name unknown]", so the user can remove it rather than lose it silently.
     */
    fun myStops(
        saved: List<SavedStop>,
        names: Map<Int, String>,
        limitReached: Boolean = false,
        arrivals: Map<Int, List<ServedArrivalUi>> = emptyMap(),
        failures: Set<Int> = emptySet(),
    ): MyStopsUi =
        MyStopsUi(
            rows =
                saved.map { stop ->
                    val alias = stop.alias?.let(SavedStopAlias::of)
                    SavedStopRowUi(
                        stopId = stop.stopId,
                        label = alias ?: names[stop.stopId] ?: UNKNOWN_STOP,
                        position = stop.position,
                        arrivals = shown(arrivals[stop.stopId].orEmpty()),
                        hasFailed = stop.stopId in failures,
                    )
                },
            limitReached = limitReached,
        )

    /**
     * LB-204: the calls a row shows — the soonest [SavedStopRowUi.ARRIVALS_PER_ROW] that are still running. A cancelled
     * call takes no slot, which is the criterion's own wording ("not counted among the 3 shown").
     */
    private fun shown(arrivals: List<ServedArrivalUi>): List<ServedArrivalUi> =
        arrivals.filterNot { it.isCancelled }.take(SavedStopRowUi.ARRIVALS_PER_ROW)

    /** The most stops the user can save (FR-FAV-05). */
    val limit: Int get() = FAVOURITES_PER_TYPE

    /** What a row says when the catalogue no longer holds its stop. */
    const val UNKNOWN_STOP = "Stop no longer in the catalogue"
}

/**
 * LB-209 (FR-NEAR-05): the stop search's state. [query] is what the user typed, kept so the screen can tell an empty
 * answer from no search at all; [results] are the stops the last answer found, each with its lines (DD-16).
 */
data class StopSearchUi(
    val query: String = "",
    val results: List<StopSearchResultUi> = emptyList(),
) {
    /** FR-NEAR-05: a search that found nothing says so, which only a query that was actually asked can. */
    val isEmptyAnswer: Boolean get() = query.isNotBlank() && results.isEmpty()
}
