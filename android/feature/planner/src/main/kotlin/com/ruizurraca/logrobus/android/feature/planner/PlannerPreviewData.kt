package com.ruizurraca.logrobus.android.feature.planner

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
}
