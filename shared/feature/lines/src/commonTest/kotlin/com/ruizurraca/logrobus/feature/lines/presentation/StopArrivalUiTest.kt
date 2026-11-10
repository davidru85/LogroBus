package com.ruizurraca.logrobus.feature.lines.presentation

import com.ruizurraca.logrobus.core.preferences.EtaFormat
import com.ruizurraca.logrobus.core.presentation.EtaDisplay
import com.ruizurraca.logrobus.transit.domain.Arrival
import com.ruizurraca.logrobus.transit.domain.Direction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * LB-105 (FR-LINE-05, spec §5.7): the next arrival per stop of the line detail, from the line's calls and the ETA
 * display rule. Each platform words and draws the [EtaDisplay] (design_guidelines.md §6.3).
 */
class StopArrivalUiTest {
    // 10:30 in Madrid (CET), and 09:30 UTC.
    private val now = Instant.parse("2026-11-03T09:30:00Z")

    private val stop38 = StopRowUi(stopId = 38, order = 1, name = "Gran Vía 12", lat = 42.46, lng = -2.44)
    private val stop40 = StopRowUi(stopId = 40, order = 2, name = "Portales", lat = 42.46, lng = -2.45)
    private val stops = listOf(stop38, stop40)

    private fun arrival(
        stopId: Int = 38,
        inTime: Duration = 4.minutes,
        isCancelled: Boolean = false,
        isLowConfidence: Boolean = false,
    ) = Arrival(
        lineId = 1,
        stopId = stopId,
        direction = Direction.OUTBOUND,
        expectedAt = now + inTime,
        isCancelled = isCancelled,
        isLowConfidence = isLowConfidence,
    )

    @Test
    fun lb105_aStopShowsTheNextArrivalInMinutes() {
        val rows = arrivalsByStop(stops, listOf(arrival(inTime = 4.minutes + 59.seconds)), now, EtaFormat.MINUTES)

        assertEquals(StopArrivalUi(EtaDisplay.Minutes(4), isLowConfidence = false, isCancelled = false), rows[38])
    }

    @Test
    fun lb105_aStopWithNoArrivalWithinThirtyMinutesShowsNothing() {
        val rows = arrivalsByStop(stops, listOf(arrival(stopId = 38)), now, EtaFormat.MINUTES)

        // The criterion's "–": the row is there, with no ETA for the platform to word.
        assertEquals(StopArrivalUi(eta = null, isLowConfidence = false, isCancelled = false), rows[40])
    }

    @Test
    fun lb105_anInaccuratePredictionIsMarkedLowConfidence() {
        val rows = arrivalsByStop(stops, listOf(arrival(isLowConfidence = true)), now, EtaFormat.MINUTES)

        assertEquals(StopArrivalUi(EtaDisplay.Minutes(4), isLowConfidence = true, isCancelled = false), rows[38])
    }

    @Test
    fun lb105_aCancelledArrivalIsStruckThroughWithItsTime() {
        val rows = arrivalsByStop(stops, listOf(arrival(isCancelled = true)), now, EtaFormat.MINUTES)

        // Struck through, not hidden: the row keeps the time it would have shown.
        assertEquals(StopArrivalUi(EtaDisplay.Minutes(4), isLowConfidence = false, isCancelled = true), rows[38])
        // A later call that still runs is the stop's next one, even when a cancelled call comes first.
        assertEquals(
            StopArrivalUi(EtaDisplay.Minutes(6), isLowConfidence = false, isCancelled = false),
            arrivalsByStop(
                stops,
                listOf(arrival(inTime = 4.minutes, isCancelled = true), arrival(inTime = 6.minutes)),
                now,
                EtaFormat.MINUTES,
            )[38],
        )
    }

    @Test
    fun lb105_anArrivalMoreThanAMinutePastIsIgnored() {
        val gone = arrival(inTime = (-61).seconds)
        val next = arrival(inTime = 4.minutes)

        assertEquals(
            StopArrivalUi(EtaDisplay.Minutes(4), isLowConfidence = false, isCancelled = false),
            arrivalsByStop(stops, listOf(gone, next), now, EtaFormat.MINUTES)[38],
        )
        // Alone, the expired call leaves the row empty rather than showing a time already gone.
        assertEquals(
            StopArrivalUi(eta = null, isLowConfidence = false, isCancelled = false),
            arrivalsByStop(stops, listOf(gone), now, EtaFormat.MINUTES)[38],
        )
    }

    @Test
    fun lb105_exactlyAMinutePastIsStillShown() {
        val rows = arrivalsByStop(stops, listOf(arrival(inTime = (-60).seconds)), now, EtaFormat.MINUTES)

        // Spec §5.7's other side of the boundary: a minute past is still arriving, so it reads as "Now".
        assertEquals(StopArrivalUi(EtaDisplay.Now, isLowConfidence = false, isCancelled = false), rows[38])
    }

    @Test
    fun lb105_theEarliestOfSeveralArrivalsWins() {
        val rows =
            arrivalsByStop(
                stops,
                listOf(arrival(inTime = 9.minutes), arrival(inTime = 4.minutes), arrival(inTime = 6.minutes)),
                now,
                EtaFormat.MINUTES,
            )

        assertEquals(EtaDisplay.Minutes(4), rows[38]?.eta)
    }

    @Test
    fun lb105_theClockFormatShowsAClockTime() {
        val rows = arrivalsByStop(stops, listOf(arrival(inTime = 4.minutes)), now, EtaFormat.CLOCK_TIME)

        assertEquals(
            StopArrivalUi(EtaDisplay.ClockTime(10, 34), isLowConfidence = false, isCancelled = false),
            rows[38],
        )
    }
}
