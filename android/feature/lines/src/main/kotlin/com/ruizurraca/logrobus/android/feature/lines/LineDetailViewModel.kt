package com.ruizurraca.logrobus.android.feature.lines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.domain.SetFavouriteLine
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailEvent
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailUiMapper
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    // Each Retry bumps it, which starts a new load.
    private val loads = MutableStateFlow(0)

    val state: StateFlow<LineDetailUiState> =
        loads
            .flatMapLatest { load ->
                val states = observeLines().map { LineDetailUiMapper.state(it, lineId) }
                // After Retry, Loading until the new load answers.
                if (load == 0) states else states.onStart { emit(LineDetailUiState.Loading) }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_DELAY_MILLIS), LineDetailUiState.Loading)

    fun onEvent(event: LineDetailEvent) {
        when (event) {
            LineDetailEvent.Retry -> loads.update { it + 1 }

            // FR-LINE-09: the toggle changes through the use case's flow.
            is LineDetailEvent.SetFavourite -> viewModelScope.launch { setFavourite(lineId, event.isFavourite) }
        }
    }

    private companion object {
        // The load outlives a configuration change, and stops this long after the screen leaves.
        const val STOP_DELAY_MILLIS = 5_000L
    }
}
