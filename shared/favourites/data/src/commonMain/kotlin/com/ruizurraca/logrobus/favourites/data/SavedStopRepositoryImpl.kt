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
import com.ruizurraca.logrobus.favourites.domain.SavedStop
import com.ruizurraca.logrobus.favourites.domain.SavedStopAlias
import com.ruizurraca.logrobus.favourites.domain.SavedStopRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val TAG = "SavedStops"

/**
 * The saved stops in the `favourite` table (spec §8.8), by stop id in decimal under [FavouriteTypes.STOP], each with
 * the alias its user gave it and its place in their order.
 *
 * One write at a time, in the order they were asked for: two quick taps on Save could otherwise land in the wrong order
 * on the I/O threads, and the limit of [FAVOURITES_PER_TYPE] is counted inside each write's transaction.
 */
class SavedStopRepositoryImpl(
    private val database: LogroBusDatabase,
    private val dispatchers: AppDispatchers,
    private val clock: AppClock,
    private val logger: Logger,
) : SavedStopRepository {
    override fun observeSavedStops(): Flow<AppResult<List<SavedStop>>> =
        database.favouriteQueries
            .selectSavedStops(FavouriteTypes.STOP)
            .asFlow()
            .mapToList(dispatchers.io)
            .map<_, AppResult<List<SavedStop>>> { rows ->
                AppResult.Success(
                    rows.mapNotNull { row ->
                        // A ref that is not a number is damage: dropped and counted, never shown.
                        val stopId = row.ref_id.toIntOrNull() ?: return@mapNotNull null
                        SavedStop(
                            stopId = stopId,
                            alias = row.label?.let(SavedStopAlias::of),
                            position = row.position.toInt(),
                        )
                    },
                )
            }
            // The database boundary (code_style.md §2.2): a failure ends the flow as a value, never a crash.
            .catch { failure ->
                logger.warn(TAG, "Reading the saved stops failed", failure)
                emit(AppResult.Failure(AppError.Data))
            }

    override suspend fun add(
        stopId: Int,
        alias: String?,
    ): AppResult<FavouriteAddition> {
        val now = clock.now()
        val label = alias?.let(SavedStopAlias::of)
        return write("Saving a stop failed") {
            // One transaction, so the count and the insert see the same stops.
            transactionWithResult {
                val saved = selectSavedStops(FavouriteTypes.STOP).executeAsList().mapNotNull { it.ref_id.toIntOrNull() }
                when {
                    stopId in saved -> {
                        FavouriteAddition.ALREADY_SAVED
                    }

                    saved.size >= FAVOURITES_PER_TYPE -> {
                        FavouriteAddition.LIMIT_REACHED
                    }

                    else -> {
                        val rows =
                            insertSavedStop(
                                id = "${FavouriteTypes.STOP}:$stopId",
                                type = FavouriteTypes.STOP,
                                refId = "$stopId",
                                label = label,
                                createdAt = now,
                            )
                        if (rows.value > 0) FavouriteAddition.ADDED else FavouriteAddition.ALREADY_SAVED
                    }
                }
            }
        }
    }

    override suspend fun rename(
        stopId: Int,
        alias: String?,
    ): AppResult<Unit> =
        write("Renaming a saved stop failed") {
            updateFavouriteLabel(label = alias?.let(SavedStopAlias::of), type = FavouriteTypes.STOP, refId = "$stopId")
            Unit
        }

    override suspend fun remove(stopId: Int): AppResult<Unit> =
        write("Removing a saved stop failed") {
            deleteFavourite(type = FavouriteTypes.STOP, refId = "$stopId")
            Unit
        }

    override suspend fun reorder(stopIds: List<Int>): AppResult<Unit> =
        write("Reordering the saved stops failed") {
            // One transaction: the stops are emitted once, in their new order, never half-moved.
            transaction {
                val saved = selectSavedStops(FavouriteTypes.STOP).executeAsList()
                val refs = saved.associateBy { it.ref_id }
                val named = stopIds.distinct().mapNotNull { refs["$it"] }
                (named + (saved - named.toSet())).forEachIndexed { position, row ->
                    updateFavouritePosition(position.toLong(), FavouriteTypes.STOP, row.ref_id)
                }
            }
            Unit
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
}
