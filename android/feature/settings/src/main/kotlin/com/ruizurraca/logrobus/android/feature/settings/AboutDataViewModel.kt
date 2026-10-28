package com.ruizurraca.logrobus.android.feature.settings

import androidx.lifecycle.ViewModel
import com.ruizurraca.logrobus.feature.settings.domain.ObserveLastSyncs
import com.ruizurraca.logrobus.feature.settings.presentation.AboutDataUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** About data (LB-404). It orchestrates only: the shared use case and mapper build the state (ADR-0007). */
class AboutDataViewModel(
    observeLastSyncs: ObserveLastSyncs,
) : ViewModel() {
    val state: StateFlow<AboutDataUiState> = MutableStateFlow(AboutDataUiState.Loading)
}
