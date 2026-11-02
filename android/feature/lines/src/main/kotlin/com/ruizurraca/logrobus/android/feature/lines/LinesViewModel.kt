package com.ruizurraca.logrobus.android.feature.lines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.presentation.LinesEvent
import com.ruizurraca.logrobus.feature.lines.presentation.LinesUiMapper
import com.ruizurraca.logrobus.feature.lines.presentation.LinesUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** The Lines list (LB-101). It orchestrates only: the shared use case and mapper build the state (ADR-0007). */
@OptIn(ExperimentalCoroutinesApi::class)
class LinesViewModel(
    observeLines: ObserveLinesWithFavourites,
) : ViewModel() {
    // Each Retry bumps it, which starts a new load, and with it a new sync (ADR-0008).
    private val loads = MutableStateFlow(0)

    // The load whose result the state shows. Coming back to the screen restarts the same load, which keeps the list
    // rather than flash Loading (spec §7.3); only a new load, after Retry, starts at Loading.
    private var shownLoad = 0

    val state: StateFlow<LinesUiState> =
        loads
            .flatMapLatest { load ->
                val states = observeLines().map(LinesUiMapper::state)
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

            // No row has a toggle yet.
            is LinesEvent.SetFavourite -> Unit
        }
    }

    private companion object {
        // The load outlives a configuration change, and stops this long after the screen leaves.
        const val STOP_DELAY_MILLIS = 5_000L
    }
}
