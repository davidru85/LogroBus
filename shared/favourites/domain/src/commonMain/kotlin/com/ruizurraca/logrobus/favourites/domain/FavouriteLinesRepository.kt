package com.ruizurraca.logrobus.favourites.domain

import com.ruizurraca.logrobus.core.common.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * The user's favourite lines (FR-FAV-01, FR-LINE-12), kept on the device. They are stored by line id, so a line that
 * leaves the catalogue stays saved until the user removes it. Failures are values, never exceptions (AGENTS.md §5, rule
 * 11).
 */
interface FavouriteLinesRepository {
    /** The favourite lines' ids in the user's order: the order they were added in, until LB-110 lets them reorder. */
    fun observeFavouriteLineIds(): Flow<AppResult<List<Int>>>

    /** Saves [lineId] at the end (FR-LINE-15). A line that is already a favourite keeps its place. */
    suspend fun add(lineId: Int): AppResult<Unit>

    suspend fun remove(lineId: Int): AppResult<Unit>
}
