package com.ruizurraca.logrobus.android.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.feature.settings.domain.DatasetSync
import com.ruizurraca.logrobus.feature.settings.domain.ObserveLastSyncs
import com.ruizurraca.logrobus.feature.settings.presentation.AboutDataEvent
import com.ruizurraca.logrobus.feature.settings.presentation.AboutDataUiMapper
import com.ruizurraca.logrobus.feature.settings.presentation.AboutDataUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** About data (LB-404). It orchestrates only: the shared use case and mapper build the state (ADR-0007). */
class AboutDataViewModel(
    observeLastSyncs: ObserveLastSyncs,
) : ViewModel() {
    val state: StateFlow<AboutDataUiState> =
        observeLastSyncs()
            .map<AppResult<List<DatasetSync>>, AboutDataUiState>(AboutDataUiMapper::state)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_DELAY_MILLIS), AboutDataUiState.Loading)

    @Suppress("UnusedParameter") // Stub: Retry comes next.
    fun onEvent(event: AboutDataEvent) = Unit

    private companion object {
        // The query outlives a configuration change, and stops this long after the screen leaves.
        const val STOP_DELAY_MILLIS = 5_000L
    }
}
