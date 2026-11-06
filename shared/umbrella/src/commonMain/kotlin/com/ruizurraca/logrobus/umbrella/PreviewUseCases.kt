package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.preferences.EtaFormat
import com.ruizurraca.logrobus.core.preferences.ThemePreference
import com.ruizurraca.logrobus.core.telemetry.NoOpAnalyticsTracker
import com.ruizurraca.logrobus.favourites.domain.FAVOURITES_PER_TYPE
import com.ruizurraca.logrobus.favourites.domain.FavouriteAddition
import com.ruizurraca.logrobus.favourites.domain.FavouriteLinesRepository
import com.ruizurraca.logrobus.feature.lines.domain.DefaultObserveLines
import com.ruizurraca.logrobus.feature.lines.domain.DefaultObserveLinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.domain.DefaultReorderFavouriteLines
import com.ruizurraca.logrobus.feature.lines.domain.DefaultSetFavouriteLine
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLines
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.domain.ReorderFavouriteLines
import com.ruizurraca.logrobus.feature.lines.domain.SetFavouriteLine
import com.ruizurraca.logrobus.feature.planner.domain.ReadInitialOrigin
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
 * of the script. By default every dataset synced at [SAMPLE_SYNC], 10:30 in Madrid, the cache holds the sample
 * lines, there are no favourite lines, location is off, and the settings are the defaults; each script replaces the
 * last, except [favouriteLine], [settings], [locationOn] and [locationOff]. Settings' own use cases come from
 * [PreviewSettings], and Near Me's and How to Get There's initial origin from [PreviewLocation]. The clock stands at
 * [SAMPLE_SYNC] too: the data synced just now.
 */
class PreviewUseCases internal constructor(
    internal val settingsPreview: PreviewSettings = PreviewSettings(),
    private val collections: MutableStateFlow<Int> = MutableStateFlow(0),
    internal val locationPreview: PreviewLocation = PreviewLocation(collections),
) : UseCases,
    SettingsUseCases by settingsPreview,
    NearMeUseCases by locationPreview,
    PlannerUseCases {
    // Null scripts a first load that never ends.
    private val lastSuccesses =
        MutableStateFlow<Map<StaticDataset, Instant>?>(StaticDataset.entries.associateWith { SAMPLE_SYNC })
    private val cachedLines = MutableStateFlow(SampleLines.all)
    private val favouriteLineIds = MutableStateFlow(emptyList<Int>())

    @Volatile private var failing = false

    // What the static sync answers; null scripts one that never ends.
    @Volatile private var sync: AppResult<Unit>? = AppResult.Success(Unit)

    /**
     * How many collections of these use cases are running, the location updates included: a test sees that leaving a
     * screen stops its own. Location access is counted only while location is on, when its updates run (LB-201).
     */
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

    /**
     * Adds line [id] to the scripted favourite lines, last, as the user's toggle would. One line per call, so Swift
     * chains plain Ints: `preview().favouriteLine(id: 10).favouriteLine(id: 4)`.
     */
    fun favouriteLine(id: Int): PreviewUseCases =
        apply { favouriteLineIds.update { ids -> if (id in ids) ids else ids + id } }

    /** How many times the cache was cleared, for a Swift test of Clear cache (LB-403). */
    val cacheClears: Int
        get() = settingsPreview.cacheClears

    /** Scripts a database that cannot be read: the use cases fail with a data error, then complete. */
    fun failing(): PreviewUseCases =
        apply {
            failing = true
            settingsPreview.failing = true
        }

    private fun script(
        lastSuccesses: Map<StaticDataset, Instant>?,
        lines: List<Line>,
        sync: AppResult<Unit>? = AppResult.Success(Unit),
    ) = apply {
        failing = false
        settingsPreview.failing = false
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
                    }.countedIn(collections)
            },
        )

    private val lines: ObserveLines =
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
                    }.countedIn(collections)
            },
        )

    override fun readInitialOrigin(): ReadInitialOrigin = locationPreview.readInitialOrigin()

    override fun clock(): AppClock = AppClock { SAMPLE_SYNC }

    override fun observeLinesWithFavourites(): ObserveLinesWithFavourites =
        DefaultObserveLinesWithFavourites(lines, favourites)

    override fun setFavouriteLine(): SetFavouriteLine = DefaultSetFavouriteLine(favourites, NoOpAnalyticsTracker)

    override fun reorderFavouriteLines(): ReorderFavouriteLines = DefaultReorderFavouriteLines(favourites)

    // In memory, like the database: a toggle or a new order in a preview or test shows at once, and the limit holds.
    private val favourites =
        object : FavouriteLinesRepository {
            override fun observeFavouriteLineIds() = favouriteLineIds.map { AppResult.Success(it) }

            override suspend fun add(lineId: Int): AppResult<FavouriteAddition> {
                if (failing) return AppResult.Failure(AppError.Data)
                var addition = FavouriteAddition.ADDED
                favouriteLineIds.update { ids ->
                    addition =
                        when {
                            lineId in ids -> FavouriteAddition.ALREADY_SAVED
                            ids.size >= FAVOURITES_PER_TYPE -> FavouriteAddition.LIMIT_REACHED
                            else -> FavouriteAddition.ADDED
                        }
                    if (addition == FavouriteAddition.ADDED) ids + lineId else ids
                }
                return AppResult.Success(addition)
            }

            override suspend fun remove(lineId: Int): AppResult<Unit> {
                favouriteLineIds.update { ids -> ids - lineId }
                return AppResult.Success(Unit)
            }

            override suspend fun reorder(lineIds: List<Int>): AppResult<Unit> {
                if (failing) return AppResult.Failure(AppError.Data)
                favouriteLineIds.update { ids ->
                    val named = lineIds.filter { it in ids }.distinct()
                    named + (ids - named.toSet())
                }
                return AppResult.Success(Unit)
            }
        }

    private companion object {
        val SAMPLE_SYNC = Instant.parse("2026-10-28T09:30:00Z")
    }
}

/**
 * Scripts the user's settings (LB-403), as if they had chosen them before. An extension, so Swift chains it like the
 * other scripts: `preview().settings(theme: .dark, etaFormat: .clockTime)`.
 */
fun PreviewUseCases.settings(
    theme: ThemePreference,
    etaFormat: EtaFormat,
): PreviewUseCases = apply { settingsPreview.script(theme, etaFormat) }

/**
 * Scripts location turned on (LB-201), as a granted prompt would: the next read of location access finds it on. An
 * extension, so Swift chains it like the other scripts: `preview().locationOn()`.
 */
fun PreviewUseCases.locationOn(): PreviewUseCases = apply { locationPreview.turnOn() }

/** Scripts location turned off (LB-201), as a revoked permission would: the running updates end. */
fun PreviewUseCases.locationOff(): PreviewUseCases = apply { locationPreview.turnOff() }

/** Counts the collections of this flow that are running in [collections]. */
internal fun <T> Flow<T>.countedIn(collections: MutableStateFlow<Int>): Flow<T> =
    onStart { collections.update { count -> count + 1 } }.onCompletion { collections.update { count -> count - 1 } }
