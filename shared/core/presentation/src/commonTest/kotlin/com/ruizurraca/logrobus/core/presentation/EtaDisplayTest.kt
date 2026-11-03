package com.ruizurraca.logrobus.core.presentation

import com.ruizurraca.logrobus.core.preferences.EtaFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Spec §5.7's display rule, in either ETA format (FR-SET-01). */
class EtaDisplayTest {
    // 10:30 in Madrid (CET), and 09:30 UTC.
    private val now = Instant.parse("2026-11-03T09:30:00Z")

    private fun shown(
        inTime: Duration,
        format: EtaFormat = EtaFormat.MINUTES,
    ) = EtaDisplay.of(now + inTime, now, format)

    @Test
    fun lb403_minutes_underAMinuteIsNow() {
        assertEquals(EtaDisplay.Now, shown(Duration.ZERO))
        assertEquals(EtaDisplay.Now, shown(59.seconds))
        // An arrival a little late on its time is still arriving; the list drops it after a minute (spec §5.7).
        assertEquals(EtaDisplay.Now, shown((-30).seconds))
    }

    @Test
    fun lb403_minutes_fromOneMinuteToAnHourAreMinutesRoundedDown() {
        assertEquals(EtaDisplay.Minutes(1), shown(60.seconds))
        assertEquals(EtaDisplay.Minutes(4), shown(4.minutes + 59.seconds))
        assertEquals(EtaDisplay.Minutes(59), shown(59.minutes + 59.seconds))
    }

    @Test
    fun lb403_minutes_anHourOrMoreAwayIsTheMadridClockTime() {
        assertEquals(EtaDisplay.ClockTime(11, 30), shown(60.minutes))
        assertEquals(EtaDisplay.ClockTime(12, 5), shown(95.minutes + 30.seconds))
    }

    @Test
    fun lb403_clockTimeFormat_showsEveryEtaAsHHmm() {
        val clockTime = EtaFormat.CLOCK_TIME

        assertEquals(EtaDisplay.ClockTime(10, 30), shown(Duration.ZERO, clockTime))
        assertEquals(EtaDisplay.ClockTime(10, 30), shown(59.seconds, clockTime))
        assertEquals(EtaDisplay.ClockTime(10, 34), shown(4.minutes + 59.seconds, clockTime))
        assertEquals(EtaDisplay.ClockTime(11, 29), shown(59.minutes + 30.seconds, clockTime))
        assertEquals(EtaDisplay.ClockTime(11, 30), shown(60.minutes, clockTime))
    }

    @Test
    fun lb403_anArrivalAcrossTheEndOfSummerTime_isMinutesApart_andItsOwnClockTime() {
        // 25 October 2026: at 01:00 UTC Madrid's clocks go back from 03:00 CEST to 02:00 CET. 00:55 UTC is 02:55 CEST,
        // and 01:05 UTC, ten minutes later, is 02:05 CET: the gap is never counted on the wall clock.
        val beforeTheChange = Instant.parse("2026-10-25T00:55:00Z")
        val afterIt = Instant.parse("2026-10-25T01:05:00Z")

        assertEquals(EtaDisplay.Minutes(10), EtaDisplay.of(afterIt, beforeTheChange, EtaFormat.MINUTES))
        assertEquals(EtaDisplay.ClockTime(2, 5), EtaDisplay.of(afterIt, beforeTheChange, EtaFormat.CLOCK_TIME))
    }

    @Test
    fun lb403_clockTimes_areOnTheMadridClockInSummerTimeToo() {
        // 18:00 UTC is 20:00 in Madrid in CEST.
        val summer = Instant.parse("2026-07-01T18:00:00Z")

        assertEquals(EtaDisplay.ClockTime(20, 10), EtaDisplay.of(summer + 10.minutes, summer, EtaFormat.CLOCK_TIME))
    }
}
