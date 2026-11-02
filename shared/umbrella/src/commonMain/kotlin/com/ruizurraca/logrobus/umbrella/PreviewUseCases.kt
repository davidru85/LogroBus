package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.telemetry.NoOpAnalyticsTracker
import com.ruizurraca.logrobus.favourites.domain.FavouriteLinesRepository
import com.ruizurraca.logrobus.feature.lines.domain.DefaultObserveLines
import com.ruizurraca.logrobus.feature.lines.domain.DefaultObserveLinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.domain.DefaultSetFavouriteLine
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLines
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.domain.SetFavouriteLine
import com.ruizurraca.logrobus.feature.settings.domain.DefaultObserveLastSyncs
import com.ruizurraca.logrobus.feature.settings.domain.ObserveLastSyncs
import com.ruizurraca.logrobus.transit.domain.Line
import com.ruizurraca.logrobus.transit.domain.LinesRepository
import com.ruizurraca.logrobus.transit.domain.StaticDataset
import com.ruizurraca.logrobus.transit.domain.StaticSyncEngine
import com.ruizurraca.logrobus.transit.domain.SyncStatusRepository
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlin.concurrent.Volatile
import kotlin.time.Instant

/**
 * The real use cases over scripted repositories (architecture.md §5.2), so previews show what the shared mappers make
 * of the script. By default every dataset synced at [SAMPLE_SYNC], 10:30 in Madrid, and the cache holds the sample
 * lines; each script replaces the last.
 */
class PreviewUseCases internal constructor() : UseCases {
    // Null scripts a first load that never ends.
    private val lastSuccesses =
        MutableStateFlow<Map<StaticDataset, Instant>?>(StaticDataset.entries.associateWith { SAMPLE_SYNC })
    private val cachedLines = MutableStateFlow(SampleLines.all)
    private val favouriteLineIds = MutableStateFlow(emptyList<Int>())
    private val collections = MutableStateFlow(0)

    @Volatile private var failing = false

    // What the static sync answers; null scripts one that never ends.
    @Volatile private var sync: AppResult<Unit>? = AppResult.Success(Unit)

    /** How many collections of these use cases are running: a test sees that leaving a screen stops its own. */
    val activeCollections: Int
        get() = collections.value

    /** Scripts a first load that never ends: the use cases emit nothing. */
    fun loading(): PreviewUseCases = script(lastSuccesses = null, lines = emptyList(), sync = null)

    /** Scripts an app that has never synced, and a sync that stored no lines. */
    fun neverSynced(): PreviewUseCases = script(lastSuccesses = emptyMap(), lines = emptyList())

    /** Scripts a first launch with no network: nothing has synced, and syncing fails with a network error. */
    fun offline(): PreviewUseCases =
        script(lastSuccesses = emptyMap(), lines = emptyList(), sync = AppResult.Failure(AppError.Network))

    /** Scripts the default again: every dataset synced, with the sample lines. Retry then loads them. */
    fun synced(): PreviewUseCases =
        script(lastSuccesses = StaticDataset.entries.associateWith { SAMPLE_SYNC }, lines = SampleLines.all)

    /** Scripts a database that cannot be read: the use cases fail with a data error, then complete. */
    fun failing(): PreviewUseCases = apply { failing = true }

    private fun script(
        lastSuccesses: Map<StaticDataset, Instant>?,
        lines: List<Line>,
        sync: AppResult<Unit>? = AppResult.Success(Unit),
    ) = apply {
        failing = false
        this.sync = sync
        this.lastSuccesses.value = lastSuccesses
        cachedLines.value = lines
    }

    override fun observeLastSyncs(): ObserveLastSyncs =
        DefaultObserveLastSyncs(
            object : SyncStatusRepository {
                override fun observeLastSuccesses() =
                    flow {
                        // Like the real repository, a failure ends the flow.
                        if (failing) {
                            emit(AppResult.Failure(AppError.Data))
                        } else {
                            emitAll(lastSuccesses.filterNotNull().map { AppResult.Success(it) })
                        }
                    }.counted()
            },
        )

    override fun observeLines(): ObserveLines =
        DefaultObserveLines(
            object : StaticSyncEngine {
                override suspend fun syncIfStale() = sync ?: awaitCancellation()
            },
            object : LinesRepository {
                override fun observeLines() =
                    flow {
                        // Like the real repository, a failure ends the flow.
                        if (failing) {
                            emit(AppResult.Failure(AppError.Data))
                        } else {
                            emitAll(cachedLines.map { AppResult.Success(it) })
                        }
                    }.counted()
            },
        )

    override fun observeLinesWithFavourites(): ObserveLinesWithFavourites =
        DefaultObserveLinesWithFavourites(observeLines(), favouriteLines)

    override fun setFavouriteLine(): SetFavouriteLine = DefaultSetFavouriteLine(favouriteLines, NoOpAnalyticsTracker)

    // In memory, like the database: a toggle in a preview or test shows at once.
    private val favouriteLines =
        object : FavouriteLinesRepository {
            override fun observeFavouriteLineIds() = favouriteLineIds.map { AppResult.Success(it) }

            override suspend fun add(lineId: Int): AppResult<Unit> {
                favouriteLineIds.update { ids -> if (lineId in ids) ids else ids + lineId }
                return AppResult.Success(Unit)
            }

            override suspend fun remove(lineId: Int): AppResult<Unit> {
                favouriteLineIds.update { ids -> ids - lineId }
                return AppResult.Success(Unit)
            }
        }

    private fun <T> Flow<T>.counted(): Flow<T> =
        onStart { collections.update { count -> count + 1 } }.onCompletion { collections.update { count -> count - 1 } }

    private companion object {
        val SAMPLE_SYNC = Instant.parse("2026-10-28T09:30:00Z")
    }
}
