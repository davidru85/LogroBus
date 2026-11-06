package com.ruizurraca.logrobus.feature.planner.presentation

import com.ruizurraca.logrobus.feature.planner.domain.PlanPlace

/**
 * How to Get There's root (design_guidelines.md §3.3). Until place search lands (LB-301 part 2), its origin and
 * destination fields only (owner decision, 2026-11-06).
 */
data class PlannerUiState(
    val origin: PlaceFieldUi = PlaceFieldUi.Empty,
    val destination: PlaceFieldUi = PlaceFieldUi.Empty,
)

/** What an origin or destination field shows. */
sealed interface PlaceFieldUi {
    /** Nothing chosen: the field shows its hint, "Where from?" or "Where to?". */
    data object Empty : PlaceFieldUi

    /** The user's location: "My location" (FR-PLAN-11). */
    data object MyLocation : PlaceFieldUi
}

/** What How to Get There's root sends its ViewModel. */
sealed interface PlannerEvent {
    /**
     * The root shows: opened, selected again, or back from the background. The location permission may have changed
     * meanwhile, in Near Me or in the system's settings, so the initial origin is read again (FR-PLAN-11).
     */
    data object Shown : PlannerEvent
}

object PlannerUiMapper {
    /** The root's fields for a new plan from [origin]: nothing is chosen for the destination yet. */
    fun state(origin: PlanPlace?): PlannerUiState =
        PlannerUiState(origin = field(origin), destination = PlaceFieldUi.Empty)

    private fun field(place: PlanPlace?): PlaceFieldUi =
        when (place) {
            null -> PlaceFieldUi.Empty
            PlanPlace.MyLocation -> PlaceFieldUi.MyLocation
        }
}
