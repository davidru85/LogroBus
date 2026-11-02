package com.ruizurraca.logrobus.feature.lines.domain

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.favourites.domain.FavouriteLinesRepository
import kotlinx.coroutines.flow.MutableStateFlow

/** The favourite lines in memory, as the database keeps them; [failing] makes every change fail. */
internal class FakeFavouriteLines(
    vararg ids: Int,
) : FavouriteLinesRepository {
    val stored = MutableStateFlow<AppResult<List<Int>>>(AppResult.Success(ids.toList()))
    var failing = false

    override fun observeFavouriteLineIds() = stored

    override suspend fun add(lineId: Int): AppResult<Unit> = change { ids -> if (lineId in ids) ids else ids + lineId }

    override suspend fun remove(lineId: Int): AppResult<Unit> = change { ids -> ids - lineId }

    private fun change(update: (List<Int>) -> List<Int>): AppResult<Unit> {
        val ids = (stored.value as? AppResult.Success)?.value
        if (failing || ids == null) return AppResult.Failure(AppError.Data)
        stored.value = AppResult.Success(update(ids))
        return AppResult.Success(Unit)
    }
}
