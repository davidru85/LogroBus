package com.ruizurraca.logrobus.transit.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * DD-05. Each case is a bus that reported its position at `recordedAt` (true UTC) and whose next stop the source gives
 * as Madrid wall-clock time labelled `Z`. 2026's transitions: 29 March and 25 October, both at 01:00 UTC.
 */
class NextStopTimeTest {
    @Test
    fun dd05_aMislabelledTimeIsReadAsMadridTime_inCest() {
        // 08:20 CEST; the next stop at 08:25 Madrid time.
        val corrected = correctNextStopTime(raw = at("2026-07-15T08:25:00Z"), recordedAt = at("2026-07-15T06:20:00Z"))

        assertEquals(at("2026-07-15T06:25:00Z"), corrected)
    }

    @Test
    fun dd05_aMislabelledTimeIsReadAsMadridTime_inCet() {
        // 08:20 CET; the next stop at 08:25 Madrid time.
        val corrected = correctNextStopTime(raw = at("2026-01-15T08:25:00Z"), recordedAt = at("2026-01-15T07:20:00Z"))

        assertEquals(at("2026-01-15T07:25:00Z"), corrected)
    }

    @Test
    fun dd05_aLateBussMislabelledTimeIsCorrectedToo_inCet() {
        // 08:20 CET, twenty minutes late for its 08:00 stop: labelled 08:00Z, only 40 minutes after the report.
        val corrected = correctNextStopTime(raw = at("2026-01-15T08:00:00Z"), recordedAt = at("2026-01-15T07:20:00Z"))

        assertEquals(at("2026-01-15T07:00:00Z"), corrected)
    }

    @Test
    fun dd05_aTimeWithinThirtyMinutesOfTheReportIsLeftAlone() {
        // Risk R4: should the source fix the label, its times stay as they are.
        listOf("2026-07-15T06:45:00Z", "2026-07-15T06:50:00Z").forEach { raw ->
            assertEquals(at(raw), correctNextStopTime(raw = at(raw), recordedAt = at("2026-07-15T06:20:00Z")))
        }
    }

    @Test
    fun dd05_aTimeBeforeTheReportIsLeftAlone() {
        val raw = at("2026-07-15T06:10:00Z")

        assertEquals(raw, correctNextStopTime(raw = raw, recordedAt = at("2026-07-15T06:20:00Z")))
    }

    @Test
    fun dd05_onTheLastSundayOfMarchTimesOnEitherSideOfTheJumpAreCorrected() {
        // 01:40 CET, next stop at 01:50 CET.
        assertEquals(
            at("2026-03-29T00:50:00Z"),
            correctNextStopTime(raw = at("2026-03-29T01:50:00Z"), recordedAt = at("2026-03-29T00:40:00Z")),
        )
        // 01:55 CET, next stop at 03:05 CEST, ten minutes later across the jump.
        assertEquals(
            at("2026-03-29T01:05:00Z"),
            correctNextStopTime(raw = at("2026-03-29T03:05:00Z"), recordedAt = at("2026-03-29T00:55:00Z")),
        )
        // 03:20 CEST, next stop at 03:25 CEST.
        assertEquals(
            at("2026-03-29T01:25:00Z"),
            correctNextStopTime(raw = at("2026-03-29T03:25:00Z"), recordedAt = at("2026-03-29T01:20:00Z")),
        )
    }

    @Test
    fun dd05_aTimeInTheHourSkippedInMarchIsReadNearestTheReport() {
        // Madrid's clock never shows 02:30 that night, so the source counted from CET (01:30 UTC) or from CEST
        // (00:30 UTC). At 01:25 CET, five minutes ahead is the one a next stop can be.
        assertEquals(
            at("2026-03-29T00:30:00Z"),
            correctNextStopTime(raw = at("2026-03-29T02:30:00Z"), recordedAt = at("2026-03-29T00:25:00Z")),
        )
    }

    @Test
    fun dd05_onTheLastSundayOfOctoberATimeAcrossTheChangeIsCorrected() {
        // 02:55 CEST, next stop at 02:05 CET, ten minutes later across the change.
        assertEquals(
            at("2026-10-25T01:05:00Z"),
            correctNextStopTime(raw = at("2026-10-25T02:05:00Z"), recordedAt = at("2026-10-25T00:55:00Z")),
        )
    }

    @Test
    fun dd05_onTheLastSundayOfOctoberTheRepeatedHourIsReadNearestTheReport() {
        // 02:25 happens twice in Madrid: at 00:25 UTC (CEST) and at 01:25 UTC (CET).
        // First pass: 02:20 CEST.
        assertEquals(
            at("2026-10-25T00:25:00Z"),
            correctNextStopTime(raw = at("2026-10-25T02:25:00Z"), recordedAt = at("2026-10-25T00:20:00Z")),
        )
        // Second pass: 02:20 CET.
        assertEquals(
            at("2026-10-25T01:25:00Z"),
            correctNextStopTime(raw = at("2026-10-25T02:25:00Z"), recordedAt = at("2026-10-25T01:20:00Z")),
        )
        // 02:55 CET, next stop at 03:05 CET, after the repeated hour.
        assertEquals(
            at("2026-10-25T02:05:00Z"),
            correctNextStopTime(raw = at("2026-10-25T03:05:00Z"), recordedAt = at("2026-10-25T01:55:00Z")),
        )
    }

    private fun at(iso: String): Instant = Instant.parse(iso)
}
