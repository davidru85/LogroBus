package com.ruizurraca.logrobus.feature.lines.domain

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.favourites.domain.FavouriteLinesRepository

/** FR-FAV-02: the edit sheet's new order of the favourite lines. An interface, because Swift calls it. */
fun interface ReorderFavouriteLines {
    suspend operator fun invoke(lineIds: List<Int>): AppResult<Unit>
}

class DefaultReorderFavouriteLines(
    private val favourites: FavouriteLinesRepository,
) : ReorderFavouriteLines {
    override suspend fun invoke(lineIds: List<Int>): AppResult<Unit> = AppResult.Success(Unit)
}
