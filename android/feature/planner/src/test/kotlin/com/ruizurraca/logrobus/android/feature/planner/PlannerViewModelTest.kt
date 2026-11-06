package com.ruizurraca.logrobus.android.feature.planner

import com.ruizurraca.logrobus.feature.planner.domain.PlanPlace
import com.ruizurraca.logrobus.feature.planner.domain.ReadInitialOrigin
import com.ruizurraca.logrobus.feature.planner.presentation.PlaceFieldUi
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerEvent
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerUiMapper
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerUiState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ADR-0007 and FR-PLAN-11: the ViewModel shows what the shared mapper makes of the initial origin, from its first
 * state, and reads it again each time the root shows.
 */
class PlannerViewModelTest {
    @Test
    fun lb301_theFirstStateIsTheSharedMappersOfTheInitialOrigin_soNothingFlashes() {
        val viewModel = PlannerViewModel { PlanPlace.MyLocation }

        assertEquals(PlannerUiMapper.state(PlanPlace.MyLocation), viewModel.state.value)
    }

    @Test
    fun lb301_whenTheRootShowsAgain_theInitialOriginIsReadAgain() {
        // The permission changes while the root is away: granted in Near Me, say, then revoked in the settings.
        var origin: PlanPlace? = null
        val viewModel = PlannerViewModel(ReadInitialOrigin { origin })
        assertEquals(PlaceFieldUi.Empty, viewModel.state.value.origin)

        origin = PlanPlace.MyLocation
        viewModel.onEvent(PlannerEvent.Shown)
        assertEquals(PlannerUiState(origin = PlaceFieldUi.MyLocation), viewModel.state.value)

        origin = null
        viewModel.onEvent(PlannerEvent.Shown)
        assertEquals(PlannerUiState(origin = PlaceFieldUi.Empty), viewModel.state.value)
    }
}
