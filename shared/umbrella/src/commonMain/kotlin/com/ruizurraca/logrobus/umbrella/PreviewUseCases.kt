package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.preferences.EtaFormat
import com.ruizurraca.logrobus.core.preferences.ThemePreference
import com.ruizurraca.logrobus.core.telemetry.NoOpAnalyticsTracker
import com.ruizurraca.logrobus.favourites.domain.FAVOURITES_PER_TYPE
import com.ruizurraca.logrobus.favourites.domain.FavouriteAddition
import com.ruizurraca.logrobus.favourites.domain.FavouriteLinesRepository
import com.ruizurraca.logrobus.feature.lines.domain.DefaultObserveLineArrivals
import com.ruizurraca.logrobus.feature.lines.domain.DefaultObserveLines
import com.ruizurraca.logrobus.feature.lines.domain.DefaultObserveLinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.domain.DefaultReorderFavouriteLines
import com.ruizurraca.logrobus.feature.lines.domain.DefaultSetFavouriteLine
import com.ruizurraca.logrobus.feature.stops.domain.ObserveStopDetail
import com.ruizurraca.logrobus.feature.stops.domain.ObserveStopArrivals
import com.ruizurraca.logrobus.feature.stops.domain.DefaultObserveStopArrivals
import com.ruizurraca.logrobus.feature.stops.domain.FetchedArrivals
import com.ruizurraca.logrobus.transit.domain.ServedLine
import com.ruizurraca.logrobus.transit.domain.Stop
import kotlinx.coroutines.flow.flowOf
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLineArrivals
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLines
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.domain.ReorderFavouriteLines
import com.ruizurraca.logrobus.feature.lines.domain.SetFavouriteLine
import com.ruizurraca.logrobus.feature.settings.domain.DefaultObserveLastSyncs
import com.ruizurraca.logrobus.feature.settings.domain.ObserveLastSyncs
import com.ruizurraca.logrobus.transit.domain.Arrival
import com.ruizurraca.logrobus.transit.domain.ArrivalsRepository
import com.ruizurraca.logrobus.transit.domain.Direction
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
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * The real use cases over scripted repositories (architecture.md §5.2), so previews show what the shared mappers make
 * of the script. By default every dataset synced at [SAMPLE_SYNC], 10:30 in Madrid, the cache holds the sample
 * lines, there are no favourite lines, location is off, and the settings are the defaults; each script replaces the
 * last, except [favouriteLine], [settings], [locationOn] and [locationOff]. Settings' own use cases come from
 * [PreviewSettings], Near Me's from [PreviewLocation], and How to Get There's from [PreviewPlanner], over the same
 * location, on a clock that stands at [SAMPLE_SYNC] too: the data synced just now.
 */
class PreviewUseCases internal constructor(
    internal val settingsPreview: PreviewSettings = PreviewSettings(),
    private val collections: MutableStateFlow<Int> = MutableStateFlow(0),
    internal val locationPreview: PreviewLocation = PreviewLocation(collections),
    internal val plannerPreview: PreviewPlanner = PreviewPlanner(locationPreview, now = SAMPLE_SYNC),
    internal val arrivalsPreview: PreviewArrivals = PreviewArrivals(),
) : UseCases,
    SettingsUseCases by settingsPreview,
    NearMeUseCases by locationPreview,
    PlannerUseCases by plannerPreview {
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

    /**
     * LB-105: how many times the detail has collected a direction's calls, so a test sees that a new direction starts
     * a collection of its own and that a format change does not. Counted apart from [activeCollections], which is
     * about the screens' flows ending.
     */
    val arrivalCollections: Int
        get() = arrivalsPreview.collections

    private val arrivals = arrivalsPreview.repository(failing = { failing })

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

    internal fun script(
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

    override fun observeLinesWithFavourites(): ObserveLinesWithFavourites =
        DefaultObserveLinesWithFavourites(lines, favourites)

    override fun observeLineArrivals(): ObserveLineArrivals = DefaultObserveLineArrivals(arrivals)

    /** LB-205: the preview's stop, scripted with [scriptStop], so a preview shows a detail without a catalogue. */
    override fun observeStopDetail(): ObserveStopDetail =
        ObserveStopDetail { stopId -> flowOf(AppResult.Success(scriptedStop ?: sampleStop(stopId))) }

    /**
     * LB-205: a stop of the sample lines, so a preview or a snapshot has a detail to draw without a catalogue. It is
     * the first stop of the sample's first line, renamed with [stopId], with that line's two headsigns (DD-16).
     */
    private fun sampleStop(stopId: Int): Stop {
        val line = SampleLines.all.first()
        val outbound = line.patterns.values.firstOrNull()
        val first = outbound?.stops?.firstOrNull()
        return Stop(
            id = stopId,
            name = first?.name ?: line.displayName,
            lat = first?.lat ?: 42.4650,
            lng = first?.lng ?: -2.4456,
            isOperational = true,
            isProvisional = false,
            servedLines =
                line.patterns.map { (direction, pattern) ->
                    ServedLine(
                        lineId = line.id,
                        code = line.code,
                        headsign = pattern.headsign,
                        direction = direction,
                        color = line.color,
                        onColor = line.onColor,
                    )
                },
        )
    }

    override fun observeStopArrivals(): ObserveStopArrivals =
        DefaultObserveStopArrivals(stops = { stopId -> observeStopDetail()(stopId) }) { _, _ ->
            flowOf(AppResult.Success(FetchedArrivals(arrivals = scriptedStopArrivals, fetchedAt = SAMPLE_SYNC)))
        }

    /** LB-205: the stop a preview shows, or the sample its lines give. */
    internal var scriptedStop: Stop? = null

    /** LB-205: the calls a preview's stop detail shows. */
    internal var scriptedStopArrivals: List<Arrival> = emptyList()

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

/** Counts the collections of this flow that are running in [collections]. */
internal fun <T> Flow<T>.countedIn(collections: MutableStateFlow<Int>): Flow<T> =
    onStart { collections.update { count -> count + 1 } }.onCompletion { collections.update { count -> count - 1 } }
