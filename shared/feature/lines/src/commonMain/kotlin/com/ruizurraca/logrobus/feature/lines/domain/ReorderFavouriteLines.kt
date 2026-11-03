package com.ruizurraca.logrobus.feature.lines.domain

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.favourites.domain.FavouriteLinesRepository

/**
 * FR-FAV-02: the edit sheet's new order of the favourite lines, saved in one change. An interface, because Swift calls
 * it and Swift tests replace it (architecture.md §4.6). Reordering counts no analytics event (NFR-OBS-03).
 */
fun interface ReorderFavouriteLines {
    suspend operator fun invoke(lineIds: List<Int>): AppResult<Unit>
}

class DefaultReorderFavouriteLines(
    private val favourites: FavouriteLinesRepository,
) : ReorderFavouriteLines {
    override suspend fun invoke(lineIds: List<Int>): AppResult<Unit> = favourites.reorder(lineIds)
}
