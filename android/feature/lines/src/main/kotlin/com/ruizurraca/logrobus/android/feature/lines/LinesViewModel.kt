package com.ruizurraca.logrobus.android.feature.lines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.feature.lines.domain.FavouriteToggle
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.domain.ReorderFavouriteLines
import com.ruizurraca.logrobus.feature.lines.domain.SetFavouriteLine
import com.ruizurraca.logrobus.feature.lines.presentation.LinesEvent
import com.ruizurraca.logrobus.feature.lines.presentation.LinesUiMapper
import com.ruizurraca.logrobus.feature.lines.presentation.LinesUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The Lines tab (LB-101, LB-103, LB-102). It orchestrates only: the shared use cases and mapper build the state
 * (ADR-0007).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LinesViewModel(
    observeLines: ObserveLinesWithFavourites,
    private val setFavourite: SetFavouriteLine,
    private val reorderFavourites: ReorderFavouriteLines,
) : ViewModel() {
    private val limit = MutableStateFlow<Int?>(null)
    val favouriteLimit: StateFlow<Int?> = limit.asStateFlow()

    // Each Retry bumps it, which starts a new load, and with it a new sync (ADR-0008).
    private val loads = MutableStateFlow(0)

    // The load whose result the state shows. Coming back to the screen restarts the same load, which keeps the list
    // rather than flash Loading (spec §7.3); only a new load, after Retry, starts at Loading.
    private var shownLoad = 0

    // The search field's text (LB-102). It filters the lines already read, so a key never starts a load.
    private val query = MutableStateFlow("")

    val state: StateFlow<LinesUiState> =
        loads
            .flatMapLatest { load ->
                val states = combine(observeLines(), query, LinesUiMapper::state)
                if (load == shownLoad) {
                    states
                } else {
                    states.onStart {
                        shownLoad = load
                        emit(LinesUiState.Loading)
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_DELAY_MILLIS), LinesUiState.Loading)

    fun onEvent(event: LinesEvent) {
        when (event) {
            LinesEvent.Retry -> loads.update { it + 1 }

            is LinesEvent.Search -> query.value = event.query

            // The carousel and the toggle change through the use case's flow (FR-LINE-15). A save that fails, a damaged
            // database the repository has logged, leaves them as they were.
            is LinesEvent.SetFavourite -> {
                viewModelScope.launch {
                    val toggle = (setFavourite(event.lineId, event.isFavourite) as? AppResult.Success)?.value
                    if (toggle is FavouriteToggle.LimitReached) limit.value = toggle.limit
                }
            }

            is LinesEvent.ReorderFavourites -> {
                viewModelScope.launch { reorderFavourites(event.lineIds) }
            }

            LinesEvent.FavouriteLimitShown -> {
                limit.value = null
            }
        }
    }

    private companion object {
        // The load outlives a configuration change, and stops this long after the screen leaves.
        const val STOP_DELAY_MILLIS = 5_000L
    }
}
