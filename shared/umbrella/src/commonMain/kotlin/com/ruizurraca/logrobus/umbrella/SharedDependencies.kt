package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.presentation.TopLevelDestination
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLineArrivals
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.domain.ReorderFavouriteLines
import com.ruizurraca.logrobus.feature.lines.domain.SetFavouriteLine
import com.ruizurraca.logrobus.feature.nearme.domain.ObserveLocationAccess
import com.ruizurraca.logrobus.feature.planner.domain.ReadInitialOrigin
import com.ruizurraca.logrobus.feature.settings.domain.ClearCache
import com.ruizurraca.logrobus.feature.settings.domain.ObserveLastSyncs
import com.ruizurraca.logrobus.feature.settings.domain.ObservePreferences
import com.ruizurraca.logrobus.feature.settings.domain.SetEtaFormat
import com.ruizurraca.logrobus.feature.settings.domain.SetTheme
import com.ruizurraca.logrobus.feature.stops.domain.DefaultObserveStopArrivals
import com.ruizurraca.logrobus.feature.stops.domain.DefaultObserveStopDetail
import com.ruizurraca.logrobus.feature.stops.domain.FetchedArrivals
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

/** Near Me's own use cases for Swift (LB-201): location access. */
interface NearMeUseCases {
    fun observeLocationAccess(): ObserveLocationAccess
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
    SettingsUseCases,
    NearMeUseCases,
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

    override fun observeStopDetail(): ObserveStopDetail = DefaultObserveStopDetail(graph().get())

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
