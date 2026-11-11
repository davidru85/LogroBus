package com.ruizurraca.logrobus.feature.nearme.presentation

import com.ruizurraca.logrobus.favourites.domain.FAVOURITES_PER_TYPE
import com.ruizurraca.logrobus.favourites.domain.SavedStop
import com.ruizurraca.logrobus.favourites.domain.SavedStopAlias

/** One row of "My stops" (FR-HOME-02, FR-STOP-08): the stop, the alias it shows and its place in the user's order. */
data class SavedStopRowUi(
    val stopId: Int,
    val label: String,
    val position: Int,
)

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
    ): MyStopsUi =
        MyStopsUi(
            rows =
                saved.map { stop ->
                    val alias = stop.alias?.let(SavedStopAlias::of)
                    SavedStopRowUi(
                        stopId = stop.stopId,
                        label = alias ?: names[stop.stopId] ?: UNKNOWN_STOP,
                        position = stop.position,
                    )
                },
            limitReached = limitReached,
        )

    /** The most stops the user can save (FR-FAV-05). */
    val limit: Int get() = FAVOURITES_PER_TYPE

    /** What a row says when the catalogue no longer holds its stop. */
    const val UNKNOWN_STOP = "Stop no longer in the catalogue"
}
