package com.ruizurraca.logrobus.feature.planner.domain

import kotlin.time.Instant

/** FR-PLAN-03: when the trip starts, "Leave now" or "Depart at" a date and time. */
sealed interface PlanDeparture {
    /** "Leave now": the trip starts when the plan is requested. */
    data object Now : PlanDeparture

    /** "Depart at": the moment the user picked. */
    data class At(
        val instant: Instant,
    ) : PlanDeparture

    /** What the plan request sends as `departureAt` (API-07). */
    fun departureAt(now: Instant): Instant = now

    companion object {
        /** "Depart at" a time picked on the Madrid clock. */
        fun atMadrid(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
        ): At = At(Instant.DISTANT_PAST)
    }
}
