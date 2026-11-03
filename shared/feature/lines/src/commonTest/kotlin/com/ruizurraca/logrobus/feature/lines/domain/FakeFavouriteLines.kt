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

    override suspend fun add(lineId: Int): AppResult<Boolean> {
        val ids = (stored.value as? AppResult.Success)?.value
        if (failing || ids == null) return AppResult.Failure(AppError.Data)
        val saved = lineId !in ids
        if (saved) stored.value = AppResult.Success(ids + lineId)
        return AppResult.Success(saved)
    }

    override suspend fun remove(lineId: Int): AppResult<Unit> {
        val ids = (stored.value as? AppResult.Success)?.value
        if (failing || ids == null) return AppResult.Failure(AppError.Data)
        stored.value = AppResult.Success(ids - lineId)
        return AppResult.Success(Unit)
    }
}
