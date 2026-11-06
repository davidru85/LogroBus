package com.ruizurraca.logrobus.feature.planner.domain

/** A trip's two ends, either of which may be unset (FR-PLAN-01). */
data class PlanPlaces(
    val origin: PlanPlace? = null,
    val destination: PlanPlace? = null,
) {
    /** FR-PLAN-08: the origin becomes the destination, and the destination the origin. */
    fun swapped(): PlanPlaces = PlanPlaces(origin = destination, destination = origin)

    /**
     * FR-PLAN-11, each time the root shows: [initialOrigin] is the origin a new plan would start from now
     * ([ReadInitialOrigin]), "My location" with the permission and none without it. Only "My location" moves here,
     * whatever else part 2's places may be.
     * - With the permission, "My location" fills an empty origin, unless the destination already holds it (after a
     *   swap, for example).
     * - Without it, "My location" leaves whichever field holds it.
     */
    fun following(initialOrigin: PlanPlace?): PlanPlaces =
        when {
            initialOrigin != PlanPlace.MyLocation -> withoutMyLocation()
            origin == null && destination != PlanPlace.MyLocation -> copy(origin = PlanPlace.MyLocation)
            else -> this
        }

    private fun withoutMyLocation(): PlanPlaces =
        PlanPlaces(origin = origin.unlessMyLocation(), destination = destination.unlessMyLocation())

    private fun PlanPlace?.unlessMyLocation(): PlanPlace? = takeUnless { it == PlanPlace.MyLocation }
}
