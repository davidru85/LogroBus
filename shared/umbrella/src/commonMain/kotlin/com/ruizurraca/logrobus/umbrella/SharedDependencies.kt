package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.presentation.TopLevelDestination
import com.ruizurraca.logrobus.favourites.domain.SavedStopRepository
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLineArrivals
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.domain.ReorderFavouriteLines
import com.ruizurraca.logrobus.feature.lines.domain.SetFavouriteLine
import com.ruizurraca.logrobus.feature.nearme.domain.ObserveLocationAccess
import com.ruizurraca.logrobus.feature.nearme.domain.ObserveMyStops
import com.ruizurraca.logrobus.feature.nearme.domain.ObserveNearYou
import com.ruizurraca.logrobus.feature.nearme.domain.SearchStops
import com.ruizurraca.logrobus.feature.planner.domain.ReadInitialOrigin
import com.ruizurraca.logrobus.feature.settings.domain.ClearCache
import com.ruizurraca.logrobus.feature.settings.domain.ObserveLastSyncs
import com.ruizurraca.logrobus.feature.settings.domain.ObservePreferences
import com.ruizurraca.logrobus.feature.settings.domain.SetEtaFormat
import com.ruizurraca.logrobus.feature.settings.domain.SetTheme
import com.ruizurraca.logrobus.feature.stops.domain.DefaultObserveStopArrivals
import com.ruizurraca.logrobus.feature.stops.domain.DefaultObserveStopDetail
import com.ruizurraca.logrobus.feature.stops.domain.FetchedArrivals
import com.ruizurraca.logrobus.feature.stops.domain.NightServiceText
import com.ruizurraca.logrobus.feature.stops.domain.ObserveStopArrivals
import com.ruizurraca.logrobus.feature.stops.domain.ObserveStopDetail
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform

/** Settings' own use cases for Swift (LB-403): the user's preferences and Clear cache. */
interface SettingsUseCases {
    fun observePreferences(): ObservePreferences

    fun setTheme(): SetTheme

    fun setEtaFormat(): SetEtaFormat

    fun clearCache(): ClearCache
}

/**
 * The saved stops' own use cases for Swift (LB-208): the stops the user saved, with their aliases and their order. A
 * group of its own, so `UseCases` stays within detekt's function limit as the features grow.
 */
interface SavedStopsUseCases {
    fun savedStops(): SavedStopRepository

    /** LB-208 (FR-HOME-02): the saved stops joined with the catalogue's names, which "My stops" shows. */
    fun observeMyStops(): ObserveMyStops
}

/** The night-service sentence's own use case for Swift (LB-206), so `UseCases` stays within detekt's limit. */
interface NightServiceUseCases {
    /** FR-STOP-06 (LB-206): the night-service sentence an empty list adds for a Búho-only stop (OQ-08). */
    fun nightServiceText(): NightServiceText
}

/** Near Me's own use cases for Swift (LB-201): location access. */
interface NearMeUseCases {
    fun observeLocationAccess(): ObserveLocationAccess

    /** LB-203 (FR-HOME-03): "Near you" from one collection, with the stops the rider can walk to. */
    fun observeNearYou(): ObserveNearYou
}

/**
 * The stop search's own use case for Swift (LB-209), kept out of Near Me's group so a location script need not answer
 * it.
 */
interface StopSearchUseCases {
    /** FR-NEAR-05, DD-16: the stops a query finds, each with the lines that reach it. */
    fun searchStops(): SearchStops
}

/**
 * How to Get There's own use cases for Swift: the origin a new plan starts from (LB-301), and the app clock, which
 * the departure's day and the pickers' start count from (LB-303, spec §8.6).
 */
interface PlannerUseCases {
    fun readInitialOrigin(): ReadInitialOrigin

    fun clock(): AppClock
}

/**
 * The use cases a Swift ViewModel can ask for (architecture.md §5.2), grouped by feature as they grow.
 * [SharedDependencies] resolves them from the graph; [SharedDependencies.preview] scripts them for previews and tests.
 */
interface UseCases :
    SavedStopsUseCases,
    SettingsUseCases,
    NearMeUseCases,
    NightServiceUseCases,
    StopSearchUseCases,
    PlannerUseCases {
    fun observeLastSyncs(): ObserveLastSyncs

    fun observeLinesWithFavourites(): ObserveLinesWithFavourites

    fun setFavouriteLine(): SetFavouriteLine

    fun reorderFavouriteLines(): ReorderFavouriteLines

    /** FR-LINE-05 (LB-105): the calls of one line and direction, polled while a screen shows them. */
    fun observeLineArrivals(): ObserveLineArrivals

    /** FR-STOP-03 (LB-205): one stop of the catalogue, with the lines that serve it, for its detail's header. */
    fun observeStopDetail(): ObserveStopDetail

    /** FR-STOP-03 to 05 (LB-205): the calls at a stop, polled while its detail shows them. */
    fun observeStopArrivals(): ObserveStopArrivals
}

/**
 * The only entry point Swift uses to reach shared code (architecture.md §5.2, ADR-0004). It hands out shared values
 * and the use cases, each feature's own group through an object of its own, as [PreviewUseCases] does; Swift never
 * reaches Koin directly.
 */
object SharedDependencies :
    UseCases,
    SettingsUseCases by GraphSettingsUseCases,
    NearMeUseCases by GraphNearMeUseCases,
    StopSearchUseCases by GraphStopSearchUseCases,
    PlannerUseCases by GraphPlannerUseCases {
    /** FR-NAV-02: the tab the app opens on. */
    val startDestination: TopLevelDestination
        get() = TopLevelDestination.start

    /** Whether the app has started the dependency graph, which the use cases come from (ADR-0014). */
    val isReady: Boolean
        get() = KoinPlatform.getKoinOrNull() != null

    override fun observeLastSyncs(): ObserveLastSyncs = graph().get()

    override fun observeLinesWithFavourites(): ObserveLinesWithFavourites = graph().get()

    override fun setFavouriteLine(): SetFavouriteLine = graph().get()

    override fun reorderFavouriteLines(): ReorderFavouriteLines = graph().get()

    override fun observeLineArrivals(): ObserveLineArrivals = graph().get()

    override fun savedStops(): SavedStopRepository = graph().get()

    override fun observeMyStops(): ObserveMyStops = graph().get()

    override fun observeStopDetail(): ObserveStopDetail = DefaultObserveStopDetail(graph().get())

    override fun nightServiceText(): NightServiceText = graph().get()

    override fun observeStopArrivals(): ObserveStopArrivals =
        DefaultObserveStopArrivals(stops = graph().get()) { stopId, lineIds ->
            // The data module answers with its own type; the feature's domain is what the use case and the screens see.
            graph()
                .get<com.ruizurraca.logrobus.transit.data.StopArrivalsReader>()
                .observeStopArrivals(stopId, lineIds)
                .map { answer ->
                    when (answer) {
                        is com.ruizurraca.logrobus.core.common.AppResult.Success -> {
                            com.ruizurraca.logrobus.core.common.AppResult.Success(
                                FetchedArrivals(answer.value.arrivals, answer.value.fetchedAt),
                            )
                        }

                        is com.ruizurraca.logrobus.core.common.AppResult.Failure -> {
                            answer
                        }
                    }
                }
        }

    /** A new set of scripted use cases, for SwiftUI previews and Swift ViewModel tests. They need no graph. */
    fun preview(): PreviewUseCases = PreviewUseCases()
}

/** Settings' own use cases, from the graph (LB-403). */
private object GraphSettingsUseCases : SettingsUseCases {
    override fun observePreferences(): ObservePreferences = graph().get()

    override fun setTheme(): SetTheme = graph().get()

    override fun setEtaFormat(): SetEtaFormat = graph().get()

    override fun clearCache(): ClearCache = graph().get()
}

/** Near Me's own use cases, from the graph (LB-201). */
private object GraphNearMeUseCases : NearMeUseCases {
    override fun observeLocationAccess(): ObserveLocationAccess = graph().get()

    override fun observeNearYou(): ObserveNearYou = graph().get()
}

/** The stop search, from the graph (LB-209). */
private object GraphStopSearchUseCases : StopSearchUseCases {
    override fun searchStops(): SearchStops = graph().get()
}

/** How to Get There's own use cases, from the graph (LB-301, LB-303). */
private object GraphPlannerUseCases : PlannerUseCases {
    override fun readInitialOrigin(): ReadInitialOrigin = graph().get()

    override fun clock(): AppClock = graph().get()
}

private fun graph() =
    checkNotNull(KoinPlatform.getKoinOrNull()) {
        "The dependency graph has not started: the app starts it at launch, and previews and tests use preview()"
    }
