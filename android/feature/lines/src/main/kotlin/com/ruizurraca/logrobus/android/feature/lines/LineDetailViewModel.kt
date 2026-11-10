package com.ruizurraca.logrobus.android.feature.lines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.preferences.PreferencesStore
import com.ruizurraca.logrobus.feature.lines.domain.FavouriteToggle
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLineArrivals
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.domain.SetFavouriteLine
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailEvent
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailUiMapper
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailUiState
import com.ruizurraca.logrobus.feature.lines.presentation.StopArrivalUi
import com.ruizurraca.logrobus.feature.lines.presentation.StopRowUi
import com.ruizurraca.logrobus.feature.lines.presentation.arrivalsByStop
import com.ruizurraca.logrobus.transit.domain.Arrival
import com.ruizurraca.logrobus.transit.domain.Direction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The detail of line [lineId] (LB-103; LB-104 adds its map, directions and stops; LB-105 each stop's next call). It
 * orchestrates only: the shared use cases and mappers build the state (ADR-0007).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LineDetailViewModel(
    private val lineId: Int,
    observeLines: ObserveLinesWithFavourites,
    private val observeArrivals: ObserveLineArrivals,
    private val preferences: PreferencesStore,
    private val clock: AppClock,
    private val setFavourite: SetFavouriteLine,
) : ViewModel() {
    private val limit = MutableStateFlow<Int?>(null)

    /**
     * FR-FAV-05: the limit a toggle has just reached, for the screen's message, until the screen says it has shown it.
     * It lives beside the state, since a toggle can reach it in any state with rows.
     */
    val favouriteLimit: StateFlow<Int?> = limit.asStateFlow()

    // Each Retry bumps it, which starts a new load.
    private val loads = MutableStateFlow(0)

    // LB-104: the direction the toggle has selected; null until one is picked, so the line's first shows.
    private val selectedDirection = MutableStateFlow<Direction?>(null)

    // The load whose result the state shows. Coming back to the screen restarts the same load, which keeps the line
    // rather than flash Loading (spec §7.3); only a new load, after Retry, starts at Loading, as in LinesViewModel.
    private var shownLoad = 0

    val state: StateFlow<LineDetailUiState> =
        loads
            .flatMapLatest { load ->
                val states =
                    combine(observeLines(), selectedDirection) { result, direction ->
                        LineDetailUiMapper.state(result, lineId, direction)
                    }
                if (load == shownLoad) {
                    states
                } else {
                    states.onStart {
                        shownLoad = load
                        emit(LineDetailUiState.Loading)
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_DELAY_MILLIS), LineDetailUiState.Loading)

    /**
     * LB-105 (FR-LINE-05): each stop's next predicted call, keyed by the stop's id, or the stop's absence of one, which
     * its row draws as the criterion's dash. It follows the direction the detail shows, and the user's ETA format
     * (FR-SET-01), which re-words the calls already received rather than asking the source again. Nothing polls until a
     * screen collects it, and it stops as the state does.
     */
    val arrivals: StateFlow<Map<Int, StopArrivalUi>> =
        stopArrivals().stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_DELAY_MILLIS), emptyMap())

    /** [arrivals]' flow, built in a function so that the settings and the clock are read where they are used. */
    private fun stopArrivals(): Flow<Map<Int, StopArrivalUi>> =
        combine(stopCalls(), preferences.preferences.map { it.etaFormat }.distinctUntilChanged()) { calls, format ->
            if (calls == null) emptyMap() else arrivalsByStop(calls.stops, calls.arrivals, clock.now(), format)
        }

    /**
     * The calls of the line for the stops the detail shows. A new direction re-subscribes through the use case's own
     * flow, so the source is asked for that direction alone; a line with no patterns asks for nothing.
     */
    private fun stopCalls(): Flow<StopCalls?> =
        state
            .map { state ->
                (state as? LineDetailUiState.Content)?.let { StopCalls(it.stops, it.direction, emptyList()) }
            }.distinctUntilChanged()
            .flatMapLatest { shown ->
                when {
                    shown == null -> {
                        flowOf(null)
                    }

                    shown.stops.isEmpty() -> {
                        flowOf(StopCalls(shown.stops, shown.direction, emptyList()))
                    }

                    else -> {
                        observeArrivals(lineId, shown.direction).map { result ->
                            StopCalls(shown.stops, shown.direction, (result as? AppResult.Success)?.value.orEmpty())
                        }
                    }
                }
            }

    fun onEvent(event: LineDetailEvent) {
        when (event) {
            LineDetailEvent.Retry -> {
                loads.update { it + 1 }
            }

            // FR-LINE-09: the toggle changes through the use case's flow.
            is LineDetailEvent.SetFavourite -> {
                // A save that fails, a damaged database the repository has logged, leaves the toggle as it was.
                viewModelScope.launch {
                    val toggle = (setFavourite(lineId, event.isFavourite) as? AppResult.Success)?.value
                    if (toggle is FavouriteToggle.LimitReached) limit.value = toggle.limit
                }
            }

            LineDetailEvent.FavouriteLimitShown -> {
                limit.value = null
            }

            // FR-LINE-04: the toggle picks a direction, and the list follows it. No reload: the patterns are cached.
            is LineDetailEvent.SelectDirection -> {
                selectedDirection.value = event.direction
            }
        }
    }

    private companion object {
        // The load outlives a configuration change, and stops this long after the screen leaves.
        const val STOP_DELAY_MILLIS = 5_000L
    }
}

/** The detail's stops for one direction, that direction, and the calls the source has sent (LB-105). */
private data class StopCalls(
    val stops: List<StopRowUi>,
    val direction: Direction,
    val arrivals: List<Arrival>,
)
