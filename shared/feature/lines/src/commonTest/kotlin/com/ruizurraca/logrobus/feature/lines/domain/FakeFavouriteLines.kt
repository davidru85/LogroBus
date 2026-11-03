package com.ruizurraca.logrobus.feature.lines.domain

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.favourites.domain.FAVOURITES_PER_TYPE
import com.ruizurraca.logrobus.favourites.domain.FavouriteAddition
import com.ruizurraca.logrobus.favourites.domain.FavouriteLinesRepository
import kotlinx.coroutines.flow.MutableStateFlow

/** The favourite lines in memory, as the database keeps them; [failing] makes every change fail. */
internal class FakeFavouriteLines(
    vararg ids: Int,
) : FavouriteLinesRepository {
    val stored = MutableStateFlow<AppResult<List<Int>>>(AppResult.Success(ids.toList()))
    var failing = false

    override fun observeFavouriteLineIds() = stored

    override suspend fun add(lineId: Int): AppResult<FavouriteAddition> {
        val ids = (stored.value as? AppResult.Success)?.value
        if (failing || ids == null) return AppResult.Failure(AppError.Data)
        val addition =
            when {
                lineId in ids -> FavouriteAddition.ALREADY_SAVED
                ids.size >= FAVOURITES_PER_TYPE -> FavouriteAddition.LIMIT_REACHED
                else -> FavouriteAddition.ADDED
            }
        if (addition == FavouriteAddition.ADDED) stored.value = AppResult.Success(ids + lineId)
        return AppResult.Success(addition)
    }

    override suspend fun remove(lineId: Int): AppResult<Unit> {
        val ids = (stored.value as? AppResult.Success)?.value
        if (failing || ids == null) return AppResult.Failure(AppError.Data)
        stored.value = AppResult.Success(ids - lineId)
        return AppResult.Success(Unit)
    }

    override suspend fun reorder(lineIds: List<Int>): AppResult<Unit> {
        val ids = (stored.value as? AppResult.Success)?.value
        if (failing || ids == null) return AppResult.Failure(AppError.Data)
        val named = lineIds.filter { it in ids }
        stored.value = AppResult.Success(named + (ids - named.toSet()))
        return AppResult.Success(Unit)
    }
}
