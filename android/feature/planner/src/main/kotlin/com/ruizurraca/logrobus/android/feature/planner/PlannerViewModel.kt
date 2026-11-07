package com.ruizurraca.logrobus.android.feature.planner

import androidx.lifecycle.ViewModel
import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.feature.planner.domain.PlanDeparture
import com.ruizurraca.logrobus.feature.planner.domain.PlanPlaces
import com.ruizurraca.logrobus.feature.planner.domain.ReadInitialOrigin
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerEvent
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerUiMapper
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * How to Get There's root (LB-301). It orchestrates only: the shared use case, `PlanPlaces`, `PlanDeparture` and the
 * mapper build the state (ADR-0007). The initial origin is read at once, so that the first frame shows it. Each time
 * the root shows, "My location" follows the permission again (FR-PLAN-11), and the swap button exchanges the trip's
 * ends (FR-PLAN-08). The departure is "Leave now" or a picked time (FR-PLAN-03), on the app clock.
 */
class PlannerViewModel(
    private val readInitialOrigin: ReadInitialOrigin,
    private val clock: AppClock,
) : ViewModel() {
    // The trip as it stands, and whether the date and time pickers are open; the state is the shared mapper's of them.
    private var places = PlanPlaces(origin = readInitialOrigin())
    private var departure: PlanDeparture = PlanDeparture.Now
    private var picking = false
    private val fields = MutableStateFlow(mapped())

    val state: StateFlow<PlannerUiState> = fields.asStateFlow()

    fun onEvent(event: PlannerEvent) {
        when (event) {
            PlannerEvent.Shown -> places = places.following(readInitialOrigin())
            PlannerEvent.Swap -> places = places.swapped()
            PlannerEvent.LeaveNow -> departure = PlanDeparture.Now
            PlannerEvent.ChooseDepartureTime -> picking = true
            PlannerEvent.CancelDepartureTime -> picking = false
            is PlannerEvent.DepartAt -> departAt(event)
        }
        fields.value = mapped()
    }

    // A picked time is the departure, and the pickers close.
    private fun departAt(time: PlannerEvent.DepartAt) {
        departure = time.departure(clock.now())
        picking = false
    }

    private fun mapped(): PlannerUiState = PlannerUiMapper.state(places, departure, picking, clock.now())
}
