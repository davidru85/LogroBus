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
import com.ruizurraca.logrobus.favourites.domain.FAVOURITES_PER_TYPE
import com.ruizurraca.logrobus.favourites.domain.FavouriteAddition
import com.ruizurraca.logrobus.favourites.domain.FavouriteLinesRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The favourite lines in the `favourite` table (spec §8.8), by line id in decimal ([FavouriteTypes.LINE]). One write at
 * a time, in the order they were asked for: two quick taps on a toggle could otherwise land in the wrong order on the
 * I/O threads.
 */
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
            .map<_, AppResult<List<Int>>> { refs -> AppResult.Success(lineIdsOf(refs)) }
            // The database boundary (code_style.md §2.2): a failure ends the flow as a value, never as a crash.
            .catch { failure ->
                logger.warn(TAG, "Reading the favourite lines failed", failure)
                emit(AppResult.Failure(AppError.Data))
            }

    override suspend fun add(lineId: Int): AppResult<FavouriteAddition> {
        val now = clock.now()
        return write("Saving a favourite line failed") {
            // One transaction, so the count and the insert see the same favourites.
            transactionWithResult {
                val saved = selectFavouriteRefs(FavouriteTypes.LINE).executeAsList()
                when {
                    "$lineId" in saved -> {
                        FavouriteAddition.ALREADY_SAVED
                    }

                    saved.size >= FAVOURITES_PER_TYPE -> {
                        FavouriteAddition.LIMIT_REACHED
                    }

                    else -> {
                        insertFavourite("${FavouriteTypes.LINE}:$lineId", FavouriteTypes.LINE, "$lineId", now)
                        FavouriteAddition.ADDED
                    }
                }
            }
        }
    }

    override suspend fun remove(lineId: Int): AppResult<Unit> =
        write("Removing a favourite line failed") { deleteFavourite(FavouriteTypes.LINE, "$lineId") }

    override suspend fun reorder(lineIds: List<Int>): AppResult<Unit> =
        write("Reordering the favourite lines failed") {
            // One transaction: the favourites are emitted once, in their new order, never half-moved.
            transaction {
                val saved = selectFavouriteRefs(FavouriteTypes.LINE).executeAsList()
                val named = lineIds.map(Int::toString).filter { it in saved }.distinct()
                (named + (saved - named.toSet())).forEachIndexed { position, ref ->
                    updateFavouritePosition(position.toLong(), FavouriteTypes.LINE, ref)
                }
            }
        }

    // Only this module writes line favourites: a ref that is not a number is damage, dropped and counted, never shown.
    private fun lineIdsOf(refs: List<String>): List<Int> {
        val ids = refs.mapNotNull(String::toIntOrNull)
        val dropped = refs.size - ids.size
        if (dropped > 0) {
            val noun = if (dropped == 1) "favourite line" else "favourite lines"
            logger.warn(TAG, "Dropped $dropped $noun with a malformed id")
        }
        return ids
    }

    /**
     * The database boundary (code_style.md §2.2): a database that cannot be written fails this change alone. Only the
     * caller's own cancellation propagates.
     */
    @Suppress("TooGenericExceptionCaught") // On purpose: see above.
    private suspend fun <T> write(
        failure: String,
        change: FavouriteQueries.() -> T,
    ): AppResult<T> =
        try {
            // A free lock is taken without suspending, so the writes queue in the order they were called.
            AppResult.Success(writes.withLock { withContext(dispatchers.io) { database.favouriteQueries.change() } })
        } catch (cause: Exception) {
            currentCoroutineContext().ensureActive()
            logger.warn(TAG, failure, cause)
            AppResult.Failure(AppError.Data)
        }

    private val writes = Mutex()

    private companion object {
        const val TAG = "Favourites"
    }
}
