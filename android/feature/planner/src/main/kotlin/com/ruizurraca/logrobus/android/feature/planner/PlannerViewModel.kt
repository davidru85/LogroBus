package com.ruizurraca.logrobus.android.feature.planner

import androidx.lifecycle.ViewModel
import com.ruizurraca.logrobus.feature.planner.domain.ReadInitialOrigin
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerEvent
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** How to Get There's root (LB-301). Stubbed until the green phase. */
class PlannerViewModel(
    @Suppress("UnusedPrivateProperty") // Used by the green phase.
    private val readInitialOrigin: ReadInitialOrigin,
) : ViewModel() {
    private val fields = MutableStateFlow(PlannerUiState())

    val state: StateFlow<PlannerUiState> = fields.asStateFlow()

    @Suppress("UnusedParameter") // Used by the green phase.
    fun onEvent(event: PlannerEvent) {}
}
