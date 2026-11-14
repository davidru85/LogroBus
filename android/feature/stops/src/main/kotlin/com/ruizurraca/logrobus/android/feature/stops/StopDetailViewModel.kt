package com.ruizurraca.logrobus.android.feature.stops

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.preferences.PreferencesStore
import com.ruizurraca.logrobus.feature.stops.domain.NightServiceText
import com.ruizurraca.logrobus.feature.stops.domain.ObserveStopArrivals
import com.ruizurraca.logrobus.feature.stops.domain.ObserveStopDetail
import com.ruizurraca.logrobus.feature.stops.domain.StopArrivals
import com.ruizurraca.logrobus.feature.stops.presentation.StopDetailUiMapper
import com.ruizurraca.logrobus.feature.stops.presentation.StopDetailUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * The detail of stop [stopId] (SC-05, FR-STOP-03 to FR-STOP-05). It orchestrates only: the shared use cases and mapper
 * build the state (ADR-0007). Each ETA is worked out against the graph's [AppClock], never the platform clock
 * (FR-ERR-06); the user's ETA format re-words the list already received rather than asking the source again
 * (FR-SET-01).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StopDetailViewModel(
    private val stopId: Int,
    private val observeDetail: ObserveStopDetail,
    private val observeArrivals: ObserveStopArrivals,
    private val preferences: PreferencesStore,
    private val clock: AppClock,
    /** LB-206 (FR-STOP-06): the night-service sentence an empty list adds for a Búho-only stop (OQ-08). */
    private val nightServiceText: NightServiceText,
) : ViewModel() {
    /**
     * LB-206: the night-service sentence for [language], which the screen passes from its own locale. Remote config
     * does not change while the screen shows, and its default is compiled in, so it never fails.
     */
    fun nightSentence(language: String): String = nightServiceText.forLanguage(language)

    // Each Retry bumps it, which starts a new load.
    private val loads = MutableStateFlow(0)

    // The load whose result the state shows. Coming back to the screen restarts the same load, which keeps the list
    // rather than flash Loading (spec §7.3); only a new load, after Retry, starts at Loading.
    private var shownLoad = 0

    val state: StateFlow<StopDetailUiState> =
        loads
            .flatMapLatest { load ->
                val states = mappedStates()
                if (load == shownLoad) {
                    states
                } else {
                    states.onStart {
                        shownLoad = load
                        emit(StopDetailUiState.Loading)
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_DELAY_MILLIS), StopDetailUiState.Loading)

    /** The state's flow, built in a function so that the settings and the clock are read where they are used. */
    private fun mappedStates(): Flow<StopDetailUiState> =
        combine(arrivals(), preferences.preferences.map { it.etaFormat }.distinctUntilChanged()) { result, format ->
            StopDetailUiMapper.state(result, stopId, clock.now(), format)
        }

    /**
     * FR-STOP-03: the stop with the lines that serve it, and then its calls, which API-06 is asked for exactly those
     * lines. The stop is read first, because the request's `lines` parameter is the served lines; a stop the catalogue
     * does not hold asks the source nothing. A stop no line reaches keeps an empty list rather than a request for one.
     */
    private fun arrivals(): Flow<AppResult<StopArrivals>> =
        observeDetail(stopId)
            .distinctUntilChanged()
            .flatMapLatest { detail ->
                when (detail) {
                    is AppResult.Failure -> {
                        flowOf(detail)
                    }

                    is AppResult.Success -> {
                        val lines = detail.value.servedLines.map { it.lineId }
                        if (lines.isEmpty()) {
                            flowOf(
                                AppResult.Success(
                                    StopArrivals(
                                        stop = detail.value,
                                        arrivals = emptyList(),
                                        fetchedAt = null,
                                        lastError = null,
                                    ),
                                ),
                            )
                        } else {
                            observeArrivals(stopId, lines)
                        }
                    }
                }
            }

    /**
     * FR-ERR-05: load again after an Error state, and the action behind "Tap to refresh" past 120 s (FR-STOP-05).
     *
     * The age of the list is worked out with the graph's [AppClock] each time an answer or the ETA format arrives, so
     * it moves with the 20 s poll rather than with a ticker of its own: a second ticker would make the state a function
     * of the wall clock, which LB-213's cadence owns.
     */
    fun retry() {
        loads.update { it + 1 }
    }

    private companion object {
        // The load outlives a configuration change, and stops this long after the screen leaves.
        const val STOP_DELAY_MILLIS = 5_000L
    }
}
