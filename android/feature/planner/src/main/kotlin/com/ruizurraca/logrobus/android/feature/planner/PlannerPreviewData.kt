package com.ruizurraca.logrobus.android.feature.planner

import com.ruizurraca.logrobus.core.presentation.MadridDateTime
import com.ruizurraca.logrobus.feature.planner.presentation.DepartureDay
import com.ruizurraca.logrobus.feature.planner.presentation.DeparturePickerUi
import com.ruizurraca.logrobus.feature.planner.presentation.DepartureUi
import com.ruizurraca.logrobus.feature.planner.presentation.PlaceFieldUi
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerUiState

/** How to Get There's root states for previews and screenshot tests. */
internal object PlannerPreviewData {
    /** With the location permission: the origin is the user's location (FR-PLAN-11). */
    val myLocation = PlannerUiState(origin = PlaceFieldUi.MyLocation, destination = PlaceFieldUi.Empty)

    /** Without it: both fields are empty. */
    val empty = PlannerUiState(origin = PlaceFieldUi.Empty, destination = PlaceFieldUi.Empty)

    /** After one swap from [myLocation]: the trip ends where the user is (FR-PLAN-08). */
    val swapped = PlannerUiState(origin = PlaceFieldUi.Empty, destination = PlaceFieldUi.MyLocation)

    /** "Depart at" a time on another day: the departure chip's longest text (FR-PLAN-03). */
    val departAt =
        myLocation.copy(departure = DepartureUi.At(MadridDateTime(2027, 1, 15, 8, 19), DepartureDay.OTHER))

    /** Choosing a time for "Leave now": the pickers open at now, 07:00 on 22 Oct 2026 (FR-PLAN-03). */
    val picking =
        myLocation.copy(
            departurePicker =
                DeparturePickerUi(start = MadridDateTime(2026, 10, 22, 7, 0), now = MadridDateTime(2026, 10, 22, 7, 0)),
        )
}
