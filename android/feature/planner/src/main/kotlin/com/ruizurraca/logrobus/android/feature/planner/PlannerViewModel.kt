package com.ruizurraca.logrobus.android.feature.planner

import androidx.lifecycle.ViewModel
import com.ruizurraca.logrobus.feature.planner.domain.ReadInitialOrigin
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerEvent
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerUiMapper
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * How to Get There's root (LB-301). It orchestrates only: the shared use case and mapper build the state (ADR-0007).
 * The initial origin is read at once, so that the first frame shows it, and again each time the root shows
 * (FR-PLAN-11).
 */
class PlannerViewModel(
    private val readInitialOrigin: ReadInitialOrigin,
) : ViewModel() {
    private val fields = MutableStateFlow(PlannerUiMapper.state(readInitialOrigin()))

    val state: StateFlow<PlannerUiState> = fields.asStateFlow()

    fun onEvent(event: PlannerEvent) {
        when (event) {
            PlannerEvent.Shown -> fields.value = PlannerUiMapper.state(readInitialOrigin())
        }
    }
}
