package com.ruizurraca.logrobus.favourites.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.common.AppDispatchers
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.warn
import com.ruizurraca.logrobus.core.database.FavouriteQueries
import com.ruizurraca.logrobus.core.database.FavouriteTypes
import com.ruizurraca.logrobus.core.database.LogroBusDatabase
import com.ruizurraca.logrobus.favourites.domain.FavouriteLinesRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** The favourite lines in the `favourite` table (spec §8.8), by line id in decimal ([FavouriteTypes.LINE]). */
class FavouriteLinesRepositoryImpl(
    private val database: LogroBusDatabase,
    private val dispatchers: AppDispatchers,
    private val clock: AppClock,
    private val logger: Logger,
) : FavouriteLinesRepository {
    override fun observeFavouriteLineIds(): Flow<AppResult<List<Int>>> =
        database.favouriteQueries
            .selectFavouriteRefs(FavouriteTypes.LINE)
            .asFlow()
            .mapToList(dispatchers.io)
            .map<_, AppResult<List<Int>>> { refs -> AppResult.Success(refs.mapNotNull(String::toIntOrNull)) }
            // The database boundary (code_style.md §2.2): a failure ends the flow as a value, never as a crash.
            .catch { failure ->
                logger.warn(TAG, "Reading the favourite lines failed", failure)
                emit(AppResult.Failure(AppError.Data))
            }

    override suspend fun add(lineId: Int): AppResult<Unit> {
        val now = clock.now()
        return write("Saving a favourite line failed") {
            insertFavourite(id = "${FavouriteTypes.LINE}:$lineId", type = FavouriteTypes.LINE, refId = "$lineId", now)
        }
    }

    override suspend fun remove(lineId: Int): AppResult<Unit> =
        write("Removing a favourite line failed") { deleteFavourite(FavouriteTypes.LINE, "$lineId") }

    /**
     * The database boundary (code_style.md §2.2): a database that cannot be written fails this change alone. Only the
     * caller's own cancellation propagates.
     */
    @Suppress("TooGenericExceptionCaught") // On purpose: see above.
    private suspend fun write(
        failure: String,
        change: FavouriteQueries.() -> Unit,
    ): AppResult<Unit> =
        try {
            withContext(dispatchers.io) { database.favouriteQueries.change() }
            AppResult.Success(Unit)
        } catch (cause: Exception) {
            currentCoroutineContext().ensureActive()
            logger.warn(TAG, failure, cause)
            AppResult.Failure(AppError.Data)
        }

    private companion object {
        const val TAG = "Favourites"
    }
}
