package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.favourites.domain.FAVOURITES_PER_TYPE
import com.ruizurraca.logrobus.favourites.domain.FavouriteAddition
import com.ruizurraca.logrobus.favourites.domain.SavedStop
import com.ruizurraca.logrobus.favourites.domain.SavedStopAlias
import com.ruizurraca.logrobus.favourites.domain.SavedStopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * LB-208 (FR-STOP-08): the saved stops of a preview or a Swift test, in memory as the favourite lines are.
 *
 * A save, a rename or a reorder shows at once, and nothing needs a database. The limit of [FAVOURITES_PER_TYPE] and
 * [SavedStopAlias]'s rule are the domain's own, so a preview behaves as the app does.
 */
internal class SavedStopScript :
    SavedStopRepository,
    SavedStopsUseCases {
    private val stops = MutableStateFlow<List<SavedStop>>(emptyList())

    /** Set to make every write fail, as an unreachable database does. */
    var failing = false

    override fun savedStops(): SavedStopRepository = this

    override fun observeSavedStops(): Flow<AppResult<List<SavedStop>>> = stops.map { AppResult.Success(it) }

    override suspend fun add(
        stopId: Int,
        alias: String?,
    ): AppResult<FavouriteAddition> {
        if (failing) return AppResult.Failure(AppError.Data)
        var addition = FavouriteAddition.ADDED
        stops.update { saved ->
            addition =
                when {
                    saved.any { it.stopId == stopId } -> FavouriteAddition.ALREADY_SAVED
                    saved.size >= FAVOURITES_PER_TYPE -> FavouriteAddition.LIMIT_REACHED
                    else -> FavouriteAddition.ADDED
                }
            if (addition == FavouriteAddition.ADDED) {
                saved + SavedStop(stopId, alias?.let(SavedStopAlias::of), saved.size)
            } else {
                saved
            }
        }
        return AppResult.Success(addition)
    }

    override suspend fun rename(
        stopId: Int,
        alias: String?,
    ): AppResult<Unit> {
        if (failing) return AppResult.Failure(AppError.Data)
        stops.update { saved ->
            saved.map { if (it.stopId == stopId) it.copy(alias = alias?.let(SavedStopAlias::of)) else it }
        }
        return AppResult.Success(Unit)
    }

    override suspend fun remove(stopId: Int): AppResult<Unit> {
        if (failing) return AppResult.Failure(AppError.Data)
        stops.update { saved -> saved.filterNot { it.stopId == stopId }.reindexed() }
        return AppResult.Success(Unit)
    }

    override suspend fun reorder(stopIds: List<Int>): AppResult<Unit> {
        if (failing) return AppResult.Failure(AppError.Data)
        stops.update { saved ->
            val named = stopIds.distinct().mapNotNull { id -> saved.firstOrNull { it.stopId == id } }
            (named + saved.filterNot { it.stopId in stopIds }).reindexed()
        }
        return AppResult.Success(Unit)
    }
}

/** The stops with their place rewritten from their order, so a list is always 0..n-1. */
private fun List<SavedStop>.reindexed(): List<SavedStop> = mapIndexed { index, stop -> stop.copy(position = index) }
