package com.ruizurraca.logrobus.android.feature.lines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.feature.lines.domain.FavouriteToggle
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.domain.SetFavouriteLine
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailEvent
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailUiMapper
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The detail of line [lineId] (LB-103; LB-104 adds its map, directions and stops). It orchestrates only: the shared use
 * cases and mapper build the state (ADR-0007).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LineDetailViewModel(
    private val lineId: Int,
    observeLines: ObserveLinesWithFavourites,
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

    // The load whose result the state shows. Coming back to the screen restarts the same load, which keeps the line
    // rather than flash Loading (spec §7.3); only a new load, after Retry, starts at Loading, as in LinesViewModel.
    private var shownLoad = 0

    val state: StateFlow<LineDetailUiState> =
        loads
            .flatMapLatest { load ->
                val states = observeLines().map { LineDetailUiMapper.state(it, lineId) }
                if (load == shownLoad) {
                    states
                } else {
                    states.onStart {
                        shownLoad = load
                        emit(LineDetailUiState.Loading)
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_DELAY_MILLIS), LineDetailUiState.Loading)

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
        }
    }

    private companion object {
        // The load outlives a configuration change, and stops this long after the screen leaves.
        const val STOP_DELAY_MILLIS = 5_000L
    }
}
