package com.ruizurraca.logrobus.android.feature.nearme

import androidx.lifecycle.ViewModel
import com.ruizurraca.logrobus.feature.nearme.domain.ObserveLocationAccess
import com.ruizurraca.logrobus.feature.nearme.presentation.NearMeEvent
import com.ruizurraca.logrobus.feature.nearme.presentation.NearMeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Near Me (LB-201). It orchestrates only: the shared use case and mapper build the state (ADR-0007). */
class NearMeViewModel(
    @Suppress("UnusedPrivateProperty") // Used by the green phase.
    private val observeLocationAccess: ObserveLocationAccess,
) : ViewModel() {
    val state: StateFlow<NearMeUiState> = MutableStateFlow(NearMeUiState())

    @Suppress("UnusedParameter") // Used by the green phase.
    fun onEvent(event: NearMeEvent) = Unit
}
