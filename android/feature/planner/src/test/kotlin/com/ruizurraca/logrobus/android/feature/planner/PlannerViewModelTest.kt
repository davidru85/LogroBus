package com.ruizurraca.logrobus.android.feature.planner

import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.presentation.MadridDateTime
import com.ruizurraca.logrobus.feature.planner.domain.PlanDeparture
import com.ruizurraca.logrobus.feature.planner.domain.PlanPlace
import com.ruizurraca.logrobus.feature.planner.domain.PlanPlaces
import com.ruizurraca.logrobus.feature.planner.domain.ReadInitialOrigin
import com.ruizurraca.logrobus.feature.planner.presentation.DepartureDay
import com.ruizurraca.logrobus.feature.planner.presentation.DeparturePickerUi
import com.ruizurraca.logrobus.feature.planner.presentation.DepartureUi
import com.ruizurraca.logrobus.feature.planner.presentation.PlaceFieldUi
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerEvent
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerUiMapper
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Instant

/**
 * ADR-0007 and FR-PLAN-11: the ViewModel shows what the shared mapper makes of the initial origin, from its first
 * state, and reads it again each time the root shows. FR-PLAN-08: the swap. FR-PLAN-03: the departure time, on the
 * app clock.
 */
class PlannerViewModelTest {
    // 07:00 on Thursday 22 Oct 2026 in Madrid (CEST).
    private val clock = AppClock { Instant.parse("2026-10-22T05:00:00Z") }

    private val eightNineteenToday = DepartureUi.At(MadridDateTime(2026, 10, 22, 8, 19), DepartureDay.TODAY)

    private fun viewModel(readInitialOrigin: ReadInitialOrigin = ReadInitialOrigin { PlanPlace.MyLocation }) =
        PlannerViewModel(readInitialOrigin, clock)

    @Test
    fun lb301_theFirstStateIsTheSharedMappersOfTheInitialOrigin_soNothingFlashes() {
        val viewModel = viewModel()

        val expected =
            PlannerUiMapper.state(
                PlanPlaces(origin = PlanPlace.MyLocation),
                PlanDeparture.Now,
                false,
                clock.now(),
            )
        assertEquals(expected, viewModel.state.value)
    }

    @Test
    fun lb301_whenTheRootShowsAgain_theInitialOriginIsReadAgain() {
        // The permission changes while the root is away: granted in Near Me, say, then revoked in the settings.
        var origin: PlanPlace? = null
        val viewModel = viewModel { origin }
        assertEquals(PlaceFieldUi.Empty, viewModel.state.value.origin)

        origin = PlanPlace.MyLocation
        viewModel.onEvent(PlannerEvent.Shown)
        assertEquals(PlannerUiState(origin = PlaceFieldUi.MyLocation), viewModel.state.value)

        origin = null
        viewModel.onEvent(PlannerEvent.Shown)
        assertEquals(PlannerUiState(origin = PlaceFieldUi.Empty), viewModel.state.value)
    }

    @Test
    fun lb304_oneSwap_exchangesTheFields_andASecondRestoresThem() {
        val viewModel = viewModel()

        viewModel.onEvent(PlannerEvent.Swap)
        assertEquals(
            PlannerUiState(origin = PlaceFieldUi.Empty, destination = PlaceFieldUi.MyLocation),
            viewModel.state.value,
        )

        viewModel.onEvent(PlannerEvent.Swap)
        assertEquals(
            PlannerUiState(origin = PlaceFieldUi.MyLocation, destination = PlaceFieldUi.Empty),
            viewModel.state.value,
        )
    }

    @Test
    fun lb304_afterASwap_theRootShowingAgain_keepsIt() {
        val viewModel = viewModel()
        viewModel.onEvent(PlannerEvent.Swap)

        // Back from another tab, or from the background, with the permission still granted.
        viewModel.onEvent(PlannerEvent.Shown)

        assertEquals(
            PlannerUiState(origin = PlaceFieldUi.Empty, destination = PlaceFieldUi.MyLocation),
            viewModel.state.value,
        )
    }

    @Test
    fun lb303_theRootStarts_leavingNow_withThePickersClosed() {
        val state = viewModel().state.value

        assertEquals(DepartureUi.Now, state.departure)
        assertNull(state.departurePicker)
    }

    @Test
    fun lb303_choosingATime_opensThePickersAtNow_onTheAppClock() {
        val viewModel = viewModel()

        viewModel.onEvent(PlannerEvent.ChooseDepartureTime)

        val now = MadridDateTime(2026, 10, 22, 7, 0)
        assertEquals(DeparturePickerUi(start = now, now = now), viewModel.state.value.departurePicker)
    }

    @Test
    fun lb303_aPickedTime_isTheDeparture_andThePickersClose() {
        val viewModel = viewModel()
        viewModel.onEvent(PlannerEvent.ChooseDepartureTime)

        viewModel.onEvent(PlannerEvent.DepartAt(year = 2026, month = 10, day = 23, hour = 8, minute = 19))

        assertEquals(
            DepartureUi.At(MadridDateTime(2026, 10, 23, 8, 19), DepartureDay.TOMORROW),
            viewModel.state.value.departure,
        )
        assertNull(viewModel.state.value.departurePicker)
    }

    @Test
    fun lb303_cancelledPickers_closeAndKeepTheDeparture() {
        val viewModel = viewModel()
        viewModel.onEvent(PlannerEvent.DepartAt(year = 2026, month = 10, day = 22, hour = 8, minute = 19))
        assertEquals(eightNineteenToday, viewModel.state.value.departure)

        viewModel.onEvent(PlannerEvent.ChooseDepartureTime)
        viewModel.onEvent(PlannerEvent.CancelDepartureTime)

        assertEquals(eightNineteenToday, viewModel.state.value.departure)
        assertNull(viewModel.state.value.departurePicker)
    }

    @Test
    fun lb303_leaveNow_afterAPickedTime_leavesNowAgain() {
        val viewModel = viewModel()
        viewModel.onEvent(PlannerEvent.DepartAt(year = 2026, month = 10, day = 22, hour = 8, minute = 19))
        assertEquals(eightNineteenToday, viewModel.state.value.departure)

        viewModel.onEvent(PlannerEvent.LeaveNow)

        assertEquals(DepartureUi.Now, viewModel.state.value.departure)
    }

    @Test
    fun lb303_aPickedTime_staysWhenTheRootShowsAgain() {
        val viewModel = viewModel()
        viewModel.onEvent(PlannerEvent.DepartAt(year = 2026, month = 10, day = 22, hour = 8, minute = 19))

        viewModel.onEvent(PlannerEvent.Shown)

        assertEquals(eightNineteenToday, viewModel.state.value.departure)
    }
}
