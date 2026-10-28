package com.ruizurraca.logrobus.android.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruizurraca.logrobus.feature.settings.domain.ObserveLastSyncs
import com.ruizurraca.logrobus.feature.settings.presentation.AboutDataEvent
import com.ruizurraca.logrobus.feature.settings.presentation.AboutDataUiMapper
import com.ruizurraca.logrobus.feature.settings.presentation.AboutDataUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** About data (LB-404). It orchestrates only: the shared use case and mapper build the state (ADR-0007). */
@OptIn(ExperimentalCoroutinesApi::class)
class AboutDataViewModel(
    observeLastSyncs: ObserveLastSyncs,
) : ViewModel() {
    // Each Retry bumps it, which starts a new read.
    private val reads = MutableStateFlow(0)

    val state: StateFlow<AboutDataUiState> =
        reads
            .flatMapLatest {
                observeLastSyncs().map(AboutDataUiMapper::state).onStart { emit(AboutDataUiState.Loading) }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_DELAY_MILLIS), AboutDataUiState.Loading)

    fun onEvent(event: AboutDataEvent) {
        when (event) {
            AboutDataEvent.Retry -> reads.update { it + 1 }
        }
    }

    private companion object {
        // The query outlives a configuration change, and stops this long after the screen leaves.
        const val STOP_DELAY_MILLIS = 5_000L
    }
}
