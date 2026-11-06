package com.ruizurraca.logrobus.android.feature.planner

import androidx.lifecycle.ViewModel
import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.feature.planner.domain.PlanPlaces
import com.ruizurraca.logrobus.feature.planner.domain.ReadInitialOrigin
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerEvent
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerUiMapper
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * How to Get There's root (LB-301). It orchestrates only: the shared use case, `PlanPlaces` and the mapper build the
 * state (ADR-0007). The initial origin is read at once, so that the first frame shows it. Each time the root shows,
 * "My location" follows the permission again (FR-PLAN-11), and the swap button exchanges the trip's ends (FR-PLAN-08).
 */
class PlannerViewModel(
    private val readInitialOrigin: ReadInitialOrigin,
    private val clock: AppClock,
) : ViewModel() {
    // The trip's ends as they stand; the state is the shared mapper's of them.
    private var places = PlanPlaces(origin = readInitialOrigin())
    private val fields = MutableStateFlow(PlannerUiMapper.state(places))

    val state: StateFlow<PlannerUiState> = fields.asStateFlow()

    fun onEvent(event: PlannerEvent) {
        places =
            when (event) {
                PlannerEvent.Shown -> places.following(readInitialOrigin())

                PlannerEvent.Swap -> places.swapped()

                PlannerEvent.LeaveNow,
                PlannerEvent.ChooseDepartureTime,
                PlannerEvent.CancelDepartureTime,
                is PlannerEvent.DepartAt,
                -> places
            }
        fields.value = PlannerUiMapper.state(places)
    }
}
