package com.ruizurraca.logrobus.core.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * AGENTS.md §5.5 and ADR-0015: times are shown on the Madrid wall clock, whatever the device time zone, across the
 * CET and CEST transitions of the last Sundays of March and October.
 */
class MadridDateTimeTest {
    private fun madrid(utc: String) = MadridDateTime.of(Instant.parse(utc))

    @Test
    fun lb405_timesAreOnTheMadridClock_inCet() {
        assertEquals(MadridDateTime(2026, 1, 15, 9, 30), madrid("2026-01-15T08:30:00Z"))
    }

    @Test
    fun lb405_timesAreOnTheMadridClock_inCest() {
        assertEquals(MadridDateTime(2026, 7, 15, 10, 30), madrid("2026-07-15T08:30:00Z"))
    }

    @Test
    fun lb405_theClockJumpsFromTwoToThreeOnTheLastSundayOfMarch() {
        assertEquals(MadridDateTime(2026, 3, 29, 1, 59), madrid("2026-03-29T00:59:00Z"))
        assertEquals(MadridDateTime(2026, 3, 29, 3, 0), madrid("2026-03-29T01:00:00Z"))
    }

    @Test
    fun lb405_theClockFallsBackFromThreeToTwoOnTheLastSundayOfOctober() {
        assertEquals(MadridDateTime(2026, 10, 25, 2, 59), madrid("2026-10-25T00:59:00Z"))
        assertEquals(MadridDateTime(2026, 10, 25, 2, 0), madrid("2026-10-25T01:00:00Z"))
    }

    @Test
    fun lb405_theMadridDateCanDifferFromTheUtcDate() {
        assertEquals(MadridDateTime(2027, 1, 1, 0, 30), madrid("2026-12-31T23:30:00Z"))
    }
}
