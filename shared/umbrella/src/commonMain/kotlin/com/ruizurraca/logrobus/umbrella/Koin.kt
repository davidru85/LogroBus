package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.common.AppDispatchers
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.ConsoleLogger
import com.ruizurraca.logrobus.core.common.LogLevel
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.ServerCorrectedClock
import com.ruizurraca.logrobus.core.common.warn
import com.ruizurraca.logrobus.core.config.CompiledDefaultsRemoteConfig
import com.ruizurraca.logrobus.core.config.RemoteConfig
import com.ruizurraca.logrobus.core.config.RemoteConfigKey
import com.ruizurraca.logrobus.core.config.pollInterval
import com.ruizurraca.logrobus.core.database.LogroBusDatabase
import com.ruizurraca.logrobus.core.database.logroBusDatabase
import com.ruizurraca.logrobus.core.network.SourceClient
import com.ruizurraca.logrobus.core.polling.DefaultPollingEngine
import com.ruizurraca.logrobus.core.polling.PollingEngine
import com.ruizurraca.logrobus.core.preferences.EtaFormat
import com.ruizurraca.logrobus.favourites.data.FavouriteLinesRepositoryImpl
import com.ruizurraca.logrobus.favourites.data.SavedStopRepositoryImpl
import com.ruizurraca.logrobus.favourites.domain.FavouriteLinesRepository
import com.ruizurraca.logrobus.favourites.domain.SavedStopRepository
import com.ruizurraca.logrobus.feature.lines.domain.DefaultObserveLineArrivals
import com.ruizurraca.logrobus.feature.lines.domain.DefaultObserveLines
import com.ruizurraca.logrobus.feature.lines.domain.DefaultObserveLinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.domain.DefaultReorderFavouriteLines
import com.ruizurraca.logrobus.feature.lines.domain.DefaultSetFavouriteLine
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLineArrivals
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLines
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.domain.ReorderFavouriteLines
import com.ruizurraca.logrobus.feature.lines.domain.SetFavouriteLine
import com.ruizurraca.logrobus.feature.nearme.domain.DefaultGetNearbyStops
import com.ruizurraca.logrobus.feature.nearme.domain.DefaultObserveLocationAccess
import com.ruizurraca.logrobus.feature.nearme.domain.DefaultObserveMyStops
import com.ruizurraca.logrobus.feature.nearme.domain.DefaultObserveNearYou
import com.ruizurraca.logrobus.feature.nearme.domain.GetNearbyStops
import com.ruizurraca.logrobus.feature.nearme.domain.ObserveLocationAccess
import com.ruizurraca.logrobus.feature.nearme.domain.ObserveMyStops
import com.ruizurraca.logrobus.feature.nearme.domain.ObserveNearYou
import com.ruizurraca.logrobus.feature.planner.domain.DefaultReadInitialOrigin
import com.ruizurraca.logrobus.feature.planner.domain.ReadInitialOrigin
import com.ruizurraca.logrobus.feature.settings.domain.ClearCache
import com.ruizurraca.logrobus.feature.settings.domain.DefaultClearCache
import com.ruizurraca.logrobus.feature.settings.domain.DefaultObserveLastSyncs
import com.ruizurraca.logrobus.feature.settings.domain.DefaultObservePreferences
import com.ruizurraca.logrobus.feature.settings.domain.DefaultSetEtaFormat
import com.ruizurraca.logrobus.feature.settings.domain.DefaultSetTheme
import com.ruizurraca.logrobus.feature.settings.domain.ObserveLastSyncs
import com.ruizurraca.logrobus.feature.settings.domain.ObservePreferences
import com.ruizurraca.logrobus.feature.settings.domain.SetEtaFormat
import com.ruizurraca.logrobus.feature.settings.domain.SetTheme
import com.ruizurraca.logrobus.feature.stops.domain.DefaultObserveStopArrivals
import com.ruizurraca.logrobus.feature.stops.domain.DefaultObserveStopDetail
import com.ruizurraca.logrobus.feature.stops.domain.FetchedArrivals
import com.ruizurraca.logrobus.feature.stops.domain.NightServiceText
import com.ruizurraca.logrobus.feature.stops.domain.NightServiceTextOf
import com.ruizurraca.logrobus.feature.stops.domain.ObserveStopArrivals
import com.ruizurraca.logrobus.feature.stops.domain.ObserveStopDetail
import com.ruizurraca.logrobus.feature.stops.presentation.StopDetailUiMapper
import com.ruizurraca.logrobus.transit.data.ArrivalsWindow
import com.ruizurraca.logrobus.transit.data.DefaultStaticSync
import com.ruizurraca.logrobus.transit.data.LinesRepositoryImpl
import com.ruizurraca.logrobus.transit.data.NearbyStopsSourceImpl
import com.ruizurraca.logrobus.transit.data.StopArrivalsReader
import com.ruizurraca.logrobus.transit.data.StopRepositoryImpl
import com.ruizurraca.logrobus.transit.data.SyncStatusRepositoryImpl
import com.ruizurraca.logrobus.transit.data.WalkDistancesApi
import com.ruizurraca.logrobus.transit.data.arrivalsRepository
import com.ruizurraca.logrobus.transit.data.patternsOfLine
import com.ruizurraca.logrobus.transit.data.stopArrivalsReader
import com.ruizurraca.logrobus.transit.domain.ArrivalsRepository
import com.ruizurraca.logrobus.transit.domain.Direction
import com.ruizurraca.logrobus.transit.domain.LinesRepository
import com.ruizurraca.logrobus.transit.domain.NearbyStopsSource
import com.ruizurraca.logrobus.transit.domain.StaticSyncEngine
import com.ruizurraca.logrobus.transit.domain.StopRepository
import com.ruizurraca.logrobus.transit.domain.SyncStatusRepository
import com.ruizurraca.logrobus.transit.domain.WalkDistances
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.dsl.onClose
import kotlin.time.Clock

/** The scope of work that lives as long as the process, such as the polling engine's loops. */
internal val APPLICATION_SCOPE = named("application")

/**
 * The shared graph (ADR-0014): the core, with one instance of each service for the whole process, then one module per
 * data and feature module. [platform] provides what only a platform can create: the [Logger], the app lifecycle, the
 * location provider, the client identity for the `User-Agent`, the SQLite driver, the settings store and the
 * caches. [telemetry] binds the telemetry contracts for the build (ADR-0009). [debug] no longer changes what a screen
 * lists: since LB-008 part 2 every build type reads the real catalogue, and only previews keep the sample lines.
 */
internal fun sharedModules(
    platform: Module,
    telemetry: Module,
    data: Module = transitDataModule,
): List<Module> =
    listOf(
        platform,
        telemetry,
        coreModule,
        data,
        favouritesDataModule,
        settingsModule,
        linesModule,
        nearMeModule,
        plannerModule,
    )

/**
 * Starts the graph of [modules] for the process (ADR-0014). A second definition of a type fails here, instead of
 * quietly replacing the first: a replaced AppClock, say, would be one the source client never corrects.
 */
internal fun startGraph(
    modules: List<Module>,
    startInitialSync: Boolean = true,
) {
    val koin =
        startKoin {
            allowOverride(false)
            modules(modules)
        }
    if (!startInitialSync) return
    // The app-start trigger of architecture.md §4.4. Fire and forget, in the application scope, because no screen waits
    // for it: the first screen runs its own sync on collection (FR-ERR-05), and this one only makes the cache warm for
    // the next launch. A failure is logged and nothing else: a background refresh never shows a UI error (FR-ERR-04).
    val scope = koin.koin.get<CoroutineScope>(APPLICATION_SCOPE)
    val engine = koin.koin.get<StaticSyncEngine>()
    val logger = koin.koin.get<Logger>()
    scope.launch {
        if (engine.syncIfStale() is AppResult.Failure) logger.warn("StaticSync", "The app-start refresh failed")
    }
}

private val coreModule =
    module {
        // One clock for the whole app: the source client corrects it, and everything else reads it (spec §8.6).
        single { ServerCorrectedClock(deviceClock = Clock.System) } bind AppClock::class
        single { AppDispatchers() }
        single<RemoteConfig> { CompiledDefaultsRemoteConfig() }
        // Stopping the graph cancels the work it started, such as polling loops.
        single(APPLICATION_SCOPE) {
            CoroutineScope(SupervisorJob() + get<AppDispatchers>().default)
        } onClose { it?.cancel() }
        single<PollingEngine> { DefaultPollingEngine(get(APPLICATION_SCOPE), get(), get<AppClock>(), get()) }
        single {
            val remoteConfig = get<RemoteConfig>()
            SourceClient(get(), get(), { remoteConfig.get(RemoteConfigKey.ApiBaseUrl) }, get())
        }
        single { logroBusDatabase(get()) }
        // LB-206 (FR-STOP-06): the night-service sentence for an empty list at a Búho-only stop (OQ-08). Here rather
        // than in the transit data module, because a preview graph replaces that one and still needs the sentence.
        single<NightServiceText> { NightServiceTextOf(get()) }
    }

private val transitDataModule =
    module {
        single<SyncStatusRepository> { SyncStatusRepositoryImpl(get(), get(), get()) }
        single<LinesRepository> { LinesRepositoryImpl(get(), get(), get()) }
        // LB-205: one stop with the lines that serve it, read from the cached catalogue so its header shows at once.
        single<StopRepository> { StopRepositoryImpl(get(), get()) }
        // LB-205: the stop detail's two use cases, which the screens ask the graph for.
        factory<ObserveStopDetail> { DefaultObserveStopDetail(get()) }
        factory<ObserveStopArrivals> {
            DefaultObserveStopArrivals(stops = get()) { stopId, lineIds ->
                get<StopArrivalsReader>().observeStopArrivals(stopId, lineIds).map { answer ->
                    when (answer) {
                        is AppResult.Success -> {
                            AppResult.Success(
                                FetchedArrivals(answer.value.arrivals, answer.value.fetchedAt),
                            )
                        }

                        is AppResult.Failure -> {
                            answer
                        }
                    }
                }
            }
        }
        // LB-205: the calls at a stop, polled while its detail shows them; FR-STOP-03's window is 60 min.
        single<StopArrivalsReader> {
            val config = get<RemoteConfig>()
            stopArrivalsReader(
                client = get(),
                polling = get(),
                window =
                    ArrivalsWindow(
                        interval = config.pollInterval(RemoteConfigKey.PollArrivalsMs),
                        previewMinutes = config.get(RemoteConfigKey.EtaPreviewMinutes),
                    ),
                logger = get(),
                patternsOf = { lineId -> patternsOfLine(get<LogroBusDatabase>(), lineId) },
            )
        }
        // LB-203: the stops near a position, and API-08's walking distances. They live with the rest of the data layer
        // so that a preview or a test replaces them the way it replaces the other source calls.
        single<NearbyStopsSource> { NearbyStopsSourceImpl(get(), get()) }
        single<WalkDistances> { WalkDistancesApi(get(), get()) }
        // LB-008 part 2: the sync over the real source, one per process so its mutex runs one sync at a time.
        single<StaticSyncEngine> { DefaultStaticSync(get(), get(), get(), get(), get(), get()).engine }
        // LB-105: the calls of a line, polled while its detail shows them. The cadence and the preview window come from
        // remote config here, where the composition root reads it, so the repository itself holds no policy.
        single<ArrivalsRepository> {
            val config = get<RemoteConfig>()
            arrivalsRepository(
                client = get(),
                polling = get(),
                window =
                    ArrivalsWindow(
                        interval = config.pollInterval(RemoteConfigKey.PollArrivalsMs),
                        // Spec §5.7: API-05 asks within 30 min. `eta_preview_minutes` (60) is FR-STOP-03's, for API-06,
                        // so the line's window is its own constant rather than another feature's setting.
                        previewMinutes = LINE_PREVIEW_MINUTES,
                    ),
                logger = get(),
                // DD-04: ET's direction code is opaque, so a call's direction is inferred from where it sits in the
                // journey. The patterns come from the cached catalogue, read once per request that needs them.
                patternsOf = { lineId -> patternsOfLine(get<LogroBusDatabase>(), lineId) },
            )
        }
    }

// Favourites are the user's own, so debug builds keep them in the real database too.
private val favouritesDataModule =
    module {
        single<FavouriteLinesRepository> { FavouriteLinesRepositoryImpl(get(), get(), get<AppClock>(), get()) }
        // LB-208 (FR-STOP-08): the stops the user saved, with their aliases and their order.
        single<SavedStopRepository> { SavedStopRepositoryImpl(get(), get(), get<AppClock>(), get()) }
    }

private val settingsModule =
    module {
        factory<ObserveLastSyncs> { DefaultObserveLastSyncs(get()) }
        factory<ObservePreferences> { DefaultObservePreferences(get()) }
        factory<SetTheme> { DefaultSetTheme(get()) }
        factory<SetEtaFormat> { DefaultSetEtaFormat(get()) }
        factory<ClearCache> { DefaultClearCache(get()) }
    }

// LB-008 part 2: every build type reads the real catalogue, so debug builds no longer list sample lines.
private val linesModule =
    module {
        factory<ObserveLines> { DefaultObserveLines(get(), get()) }
        factory<ObserveLinesWithFavourites> { DefaultObserveLinesWithFavourites(get(), get()) }
        // LB-105 (FR-LINE-05): the calls of a line, which the detail's stops show as their next arrival.
        factory<ObserveLineArrivals> { DefaultObserveLineArrivals(get()) }
        factory<SetFavouriteLine> { DefaultSetFavouriteLine(get(), get()) }
        factory<ReorderFavouriteLines> { DefaultReorderFavouriteLines(get()) }
    }

// Near Me (LB-201): location access, over the platform's location provider.
private val nearMeModule =
    module {
        factory<ObserveLocationAccess> { DefaultObserveLocationAccess(get()) }
        // LB-203: "Near you" from one collection, so the device is asked for fixes once (FR-LOC-03). The source and
        // API-08 live with the rest of the data layer (transitDataModule), so a preview can replace them.
        factory<GetNearbyStops> { DefaultGetNearbyStops(get(), get()) }
        factory<ObserveNearYou> { DefaultObserveNearYou(get(), get()) }
        // LB-208 (FR-HOME-02): "My stops", the saved stops and the catalogue's names for them. LB-204 adds each
        // stop's calls: the same use case the stop detail collects, so a row here words a call as that screen does and
        // no feature depends on another (ADR-0007).
        factory<ObserveMyStops> {
            val stops = get<StopRepository>()
            val arrivals = get<ObserveStopArrivals>()
            DefaultObserveMyStops(saved = get(), stops = stops) { stopId ->
                // A `flow` builder, so the lines are read when this stop's row is collected, not when the section is
                // built, and the lambda itself stays a plain function.
                flow {
                    val lineIds =
                        (stops.observeStop(stopId).first() as? AppResult.Success)
                            ?.value
                            ?.servedLines
                            ?.map { it.lineId }
                            .orEmpty()
                    emitAll(
                        arrivals(stopId, lineIds).map { answer ->
                            when (answer) {
                                is AppResult.Success -> {
                                    AppResult.Success(
                                        // FR-ERR-06: the rows are measured against the answer's own instant. An
                                        // answer with none cannot be aged, so the row shows no calls rather than
                                        // inventing a clock the graph did not give.
                                        answer.value.fetchedAt
                                            ?.let { fetchedAt ->
                                                StopDetailUiMapper.rows(
                                                    answer.value.arrivals,
                                                    fetchedAt,
                                                    EtaFormat.MINUTES,
                                                )
                                            }.orEmpty(),
                                    )
                                }

                                is AppResult.Failure -> {
                                    answer
                                }
                            }
                        },
                    )
                }
            }
        }
    }

// How to Get There (LB-301): the initial origin, from the platform's location permission.
private val plannerModule =
    module {
        factory<ReadInitialOrigin> { DefaultReadInitialOrigin(get()) }
    }

/** `architecture.md` §7: debug builds log from the debug level, release builds errors only. */
internal fun appLogger(debug: Boolean): Logger = ConsoleLogger(minLevel(debug))

/** The same, printing to [output], for tests. */
internal fun appLogger(
    debug: Boolean,
    output: (String) -> Unit,
): Logger = ConsoleLogger(minLevel(debug), output)

private fun minLevel(debug: Boolean): LogLevel = if (debug) LogLevel.Debug else LogLevel.Error

/** LB-105, spec §5.7 and §8.7: the calls API-05 returns are those within 30 min of now. */
private const val LINE_PREVIEW_MINUTES = 30
