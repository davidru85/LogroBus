package com.ruizurraca.logrobus.feature.planner.domain

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * FR-PLAN-03: "Leave now" or "Depart at" a date and time, sent as `departureAt` in UTC. A picked time is on the Madrid
 * clock, whatever the device's zone (AGENTS.md §5.5), so CET and CEST both hold, and so do the days the clocks change.
 */
class PlanDepartureTest {
    private val now = Instant.parse("2026-11-06T17:42:31.250Z")

    @Test
    fun lb303_leaveNow_departsAtTheAppClocksNow() {
        assertEquals(now, PlanDeparture.Now.departureAt(now))
    }

    @Test
    fun lb303_departAt0819On22Oct2026_departsAt0619Utc_inSummerTime() {
        val departure = PlanDeparture.atMadrid(LocalDateTime(2026, 10, 22, 8, 19), now)

        assertEquals(Instant.parse("2026-10-22T06:19:00.000Z"), departure.departureAt(now))
    }

    @Test
    fun lb303_departAt0819On15Jan2027_departsAt0719Utc_inWinterTime() {
        val departure = PlanDeparture.atMadrid(LocalDateTime(2027, 1, 15, 8, 19), now)

        assertEquals(Instant.parse("2027-01-15T07:19:00.000Z"), departure.departureAt(now))
    }

    @Test
    fun lb303_departAt_inTheHourSkippedOnTheLastSundayOfMarch_movesOnByThatHour() {
        // 29 Mar 2026: at 02:00 CET the clocks go to 03:00 CEST, so 02:30 never shows. It means 03:30 CEST.
        val departure = PlanDeparture.atMadrid(LocalDateTime(2026, 3, 29, 2, 30), now)

        assertEquals(Instant.parse("2026-03-29T01:30:00Z"), departure.departureAt(now))
    }

    @Test
    fun lb303_departAt_inTheHourRepeatedOnTheLastSundayOfOctober_isTheFirst_inSummerTime() {
        // 25 Oct 2026: at 03:00 CEST the clocks go back to 02:00 CET, so 02:30 shows twice. Picked the evening before,
        // it means the first one.
        val eveningBefore = Instant.parse("2026-10-24T20:00:00Z")
        val departure = PlanDeparture.atMadrid(LocalDateTime(2026, 10, 25, 2, 30), eveningBefore)

        assertEquals(Instant.parse("2026-10-25T00:30:00Z"), departure.departureAt(eveningBefore))
    }

    @Test
    fun lb303_departAt_inTheRepeatedHour_onceItsFirstPassHasGoneBy_isTheSecond_inWinterTime() {
        // 02:40 CET, the hour's second pass: 02:50 an hour ago has gone by, so 02:50 means the one ten minutes away.
        val secondPass = Instant.parse("2026-10-25T01:40:00Z")
        val departure = PlanDeparture.atMadrid(LocalDateTime(2026, 10, 25, 2, 50), secondPass)

        assertEquals(Instant.parse("2026-10-25T01:50:00Z"), departure.departureAt(secondPass))
    }

    @Test
    fun lb303_departAt_departsAtThePickedTime_whateverTheTimeNow() {
        val departure = PlanDeparture.atMadrid(LocalDateTime(2026, 10, 22, 8, 19), now)

        assertEquals(departure.departureAt(now), departure.departureAt(Instant.parse("2027-06-01T00:00:00Z")))
    }
}
