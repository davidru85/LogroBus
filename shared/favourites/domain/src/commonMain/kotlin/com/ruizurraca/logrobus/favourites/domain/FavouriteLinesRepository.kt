package com.ruizurraca.logrobus.favourites.domain

import com.ruizurraca.logrobus.core.common.AppResult
import kotlinx.coroutines.flow.Flow

/** FR-FAV-05: the most favourites of one type the user can save: lines, stops or places (requirements.md §3.6). */
const val FAVOURITES_PER_TYPE: Int = 20

/** What saving a favourite did. */
enum class FavouriteAddition {
    /** It is saved, last. */
    ADDED,

    /** It already was a favourite, and keeps its place. */
    ALREADY_SAVED,

    /** FR-FAV-05: [FAVOURITES_PER_TYPE] of its type are saved, so nothing was. */
    LIMIT_REACHED,
}

/**
 * The user's favourite lines (FR-FAV-01, FR-LINE-12), kept on the device. They are stored by line id, so a line that
 * leaves the catalogue stays saved, with no card, until it comes back; LB-110's edit mode lets the user remove it.
 * Changes apply in the order they are made. Failures are values, never exceptions (AGENTS.md §5, rule 11).
 */
interface FavouriteLinesRepository {
    /** The favourite lines' ids in the user's order: the order they were added in, until LB-110 lets them reorder. */
    fun observeFavouriteLineIds(): Flow<AppResult<List<Int>>>

    /**
     * Saves [lineId] at the end (FR-LINE-15): `true`. A line that is already a favourite keeps its place, and nothing
     * is saved: `false`.
     */
    suspend fun add(lineId: Int): AppResult<FavouriteAddition>

    suspend fun remove(lineId: Int): AppResult<Unit>

    /** FR-FAV-02: puts the favourite lines in the order of [lineIds]. */
    suspend fun reorder(lineIds: List<Int>): AppResult<Unit>
}
