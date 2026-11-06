package com.ruizurraca.logrobus.feature.planner.domain

import com.ruizurraca.logrobus.core.common.madridTimeZone
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlin.time.Instant

/** FR-PLAN-03: when the trip starts, "Leave now" or "Depart at" a date and time. */
sealed interface PlanDeparture {
    /** "Leave now": the trip starts when the plan is requested. */
    data object Now : PlanDeparture

    /** "Depart at": the moment the user picked. */
    data class At(
        val instant: Instant,
    ) : PlanDeparture

    /**
     * What the plan request sends as `departureAt` (API-07), in UTC: [now], the app clock's when the plan is requested
     * (spec §8.6), for "Leave now", and the picked moment for "Depart at".
     */
    fun departureAt(now: Instant): Instant =
        when (this) {
            Now -> now
            is At -> instant
        }

    companion object {
        /**
         * "Depart at" a time picked on the Madrid clock, whatever the device's zone (AGENTS.md §5.5): 08:19 on
         * 22 Oct 2026 is 06:19 UTC (CEST), and on 15 Jan 2027 07:19 UTC (CET). A time in the hour skipped on the last
         * Sunday of March moves on by that hour; one in the hour repeated on the last Sunday of October is the first,
         * in summer time.
         */
        fun atMadrid(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
            now: Instant,
        ): At = At(LocalDateTime(year, month, day, hour, minute).toInstant(madridTimeZone))
    }
}
