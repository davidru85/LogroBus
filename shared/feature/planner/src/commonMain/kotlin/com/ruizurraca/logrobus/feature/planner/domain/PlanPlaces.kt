package com.ruizurraca.logrobus.feature.planner.domain

/** A trip's two ends, either of which may be unset (FR-PLAN-01). */
data class PlanPlaces(
    val origin: PlanPlace? = null,
    val destination: PlanPlace? = null,
) {
    /** FR-PLAN-08. */
    fun swapped(): PlanPlaces = this

    /** FR-PLAN-11, each time the root shows. */
    fun following(initialOrigin: PlanPlace?): PlanPlaces = this
}
