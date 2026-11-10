package com.ruizurraca.logrobus.feature.stops.presentation

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.preferences.EtaFormat
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.core.presentation.EtaDisplay
import com.ruizurraca.logrobus.feature.stops.domain.StopArrivalRow
import com.ruizurraca.logrobus.feature.stops.domain.StopArrivals
import com.ruizurraca.logrobus.transit.domain.Direction
import com.ruizurraca.logrobus.transit.domain.ServedLine
import com.ruizurraca.logrobus.transit.domain.Stop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * LB-205 (FR-STOP-03 to FR-STOP-05, spec §5.7): the stop detail's arrivals and its freshness, from one answer of
 * API-06. The answers are built by hand: what this mapper reads is the domain, and decoding belongs to the repository's
 * own tests.
 */
class StopDetailUiMapperTest {
    // 10:30 in Madrid (CET) and 09:30 UTC, the same instant the other arrival tests use.
    private val now = Instant.parse("2026-11-03T09:30:00Z")

    private val stop38 =
        Stop(
            id = 38,
            name = "Gran Vía 12",
            lat = 42.46,
            lng = -2.44,
            isOperational = true,
            isProvisional = false,
            servedLines =
                listOf(
                    ServedLine(
                        lineId = 1,
                        code = "1",
                        headsign = "El Arco",
                        direction = Direction.OUTBOUND,
                        color = 0xFF0FDCFC,
                        onColor = 0xFF000000,
                    ),
                    ServedLine(
                        lineId = 33,
                        code = "33",
                        headsign = "Varea",
                        direction = Direction.INBOUND,
                        color = 0xFFF9A825,
                        onColor = 0xFF000000,
                    ),
                ),
        )

    private fun row(
        inTime: Duration = 4.minutes,
        isCancelled: Boolean = false,
        isLowConfidence: Boolean = false,
        lineCode: String = "1",
    ) = StopArrivalRow(
        lineCode = lineCode,
        color = if (lineCode == "1") 0xFF0FDCFC else 0xFFF9A825,
        onColor = 0xFF000000,
        headsign = if (lineCode == "1") "El Arco" else "Varea",
        expectedAt = now + inTime,
        isCancelled = isCancelled,
        isLowConfidence = isLowConfidence,
    )

    private fun arrivals(
        vararg rows: StopArrivalRow,
        fetchedAt: Instant? = now,
        stop: Stop = stop38,
        lastError: AppError? = null,
    ) = StopArrivals(stop = stop, arrivals = rows.toList(), fetchedAt = fetchedAt, lastError = lastError)

    private fun content(
        answers: StopArrivals,
        format: EtaFormat = EtaFormat.MINUTES,
    ) =
        StopDetailUiMapper.state(
            AppResult.Success(answers),
            stopId = 38,
            now = now,
            format = format,
        ) as StopDetailUiState.Content

    @Test
    fun lb205_thirtySecondsFourMinutesAndSeventyFiveMinutesShowNowFourMinutesAndAClockTime() {
        // LB-205's own criterion, in one list: 30 s, 4 min and 75 min show "Now", "4 min" and "HH:mm".
        val rows =
            StopDetailUiMapper.rows(
                listOf(row(inTime = 30.seconds), row(inTime = 4.minutes), row(inTime = 75.minutes)),
                now,
                EtaFormat.MINUTES,
            )

        assertEquals(
            listOf(EtaDisplay.Now, EtaDisplay.Minutes(4), EtaDisplay.ClockTime(hour = 11, minute = 45)),
            rows.map { it.eta },
        )
    }

    @Test
    fun lb205_anArrivalInThirtySeconds_isNow() {
        val rows = StopDetailUiMapper.rows(listOf(row(inTime = 30.seconds)), now, EtaFormat.MINUTES)

        assertEquals(EtaDisplay.Now, rows.single().eta)
    }

    @Test
    fun lb205_anArrivalInFourMinutes_isFourMinutes() {
        // 4 min 59 s rounds down, as spec §5.7 asks.
        val rows = StopDetailUiMapper.rows(listOf(row(inTime = 4.minutes + 59.seconds)), now, EtaFormat.MINUTES)

        assertEquals(EtaDisplay.Minutes(4), rows.single().eta)
    }

    @Test
    fun lb205_anArrivalInSeventyFiveMinutes_isAClockTime() {
        // 09:30 UTC is 10:30 in Madrid; 75 min later is 11:45.
        val rows = StopDetailUiMapper.rows(listOf(row(inTime = 75.minutes)), now, EtaFormat.MINUTES)

        assertEquals(EtaDisplay.ClockTime(hour = 11, minute = 45), rows.single().eta)
    }

    @Test
    fun lb205_anArrivalMoreThanSixtySecondsInThePast_isRemoved() {
        val rows =
            StopDetailUiMapper.rows(
                listOf(row(inTime = (-61).seconds, lineCode = "1"), row(inTime = 4.minutes, lineCode = "33")),
                now,
                EtaFormat.MINUTES,
            )

        // The badge is what tells them apart: only the live one is left.
        assertEquals(listOf("33"), rows.map { it.lineCode })
    }

    @Test
    fun lb205_anArrivalExactlySixtySecondsPast_isStillShown() {
        // Spec §5.7 removes an arrival *more than* 60 s past.
        val rows = StopDetailUiMapper.rows(listOf(row(inTime = (-60).seconds)), now, EtaFormat.MINUTES)

        assertEquals(listOf(EtaDisplay.Now), rows.map { it.eta })
    }

    @Test
    fun lb205_aCancelledArrivalIsStruckThroughWithItsBadgeAndTime() {
        val rows = StopDetailUiMapper.rows(listOf(row(inTime = 4.minutes, isCancelled = true)), now, EtaFormat.MINUTES)

        // Struck through, never hidden (design_guidelines.md §6.3): it keeps its row, its badge and its ETA.
        assertEquals(
            listOf(
                StopArrivalUi(
                    lineCode = "1",
                    color = 0xFF0FDCFC,
                    onColor = 0xFF000000,
                    headsign = "El Arco",
                    eta = EtaDisplay.Minutes(4),
                    isCancelled = true,
                    isLowConfidence = false,
                ),
            ),
            rows,
        )
    }

    @Test
    fun lb205_anInaccuratePredictionKeepsItsLowConfidenceFlag() {
        val row = StopDetailUiMapper.rows(listOf(row(isLowConfidence = true)), now, EtaFormat.MINUTES).single()

        assertEquals(true, row.isLowConfidence)
        assertEquals(false, row.isCancelled)
    }

    @Test
    fun lb205_theRowsComeInChronologicalOrder() {
        val rows =
            StopDetailUiMapper.rows(
                listOf(row(inTime = 9.minutes, lineCode = "33"), row(inTime = 2.minutes, lineCode = "1")),
                now,
                EtaFormat.MINUTES,
            )

        assertEquals(listOf(EtaDisplay.Minutes(2), EtaDisplay.Minutes(9)), rows.map { it.eta })
    }

    @Test
    fun lb205_theStopAndItsServedLinesComeFromTheAnswer() {
        val state = content(arrivals(row()))

        assertEquals(stop38, state.stop)
        assertEquals(listOf(1, 33), state.stop.servedLines.map { it.lineId })
    }

    @Test
    fun lb205_fortyNineSecondsOld_isNotStale() {
        val state = content(arrivals(row(), fetchedAt = now - 49.seconds))

        assertEquals(StopUpdatedUi(ageSeconds = 49, isStale = false, needsRefresh = false), state.updated)
    }

    @Test
    fun lb205_fiftySecondsOld_isStaleWithItsAge() {
        // LB-205: at 50 s the list says "Updated 50 s ago" in the warning colour.
        val state = content(arrivals(row(inTime = 4.minutes), fetchedAt = now - 50.seconds))

        assertEquals(StopUpdatedUi(ageSeconds = 50, isStale = true, needsRefresh = false), state.updated)
        // Stale is not expired: the ETAs still count minutes.
        assertEquals(EtaDisplay.Minutes(4), state.arrivals.single().eta)
    }

    @Test
    fun lb205_exactlyOneHundredAndTwentySecondsOld_keepsTheEtaRule() {
        // Spec §5.7 expires data *over* 120 s.
        val state = content(arrivals(row(inTime = 6.minutes), fetchedAt = now - 120.seconds))

        assertEquals(StopUpdatedUi(ageSeconds = 120, isStale = true, needsRefresh = false), state.updated)
        assertEquals(EtaDisplay.Minutes(6), state.arrivals.single().eta)
    }

    @Test
    fun lb205_pastTwoMinutesTheEtasSwitchToClockTimesWhateverTheUserChose() {
        // 121 s old is past the limit; 09:30 UTC + 6 min = 09:36 UTC, which is 10:36 in Madrid. FR-STOP-05 and the
        // criterion: over 120 s the ETAs switch to clock times, so the user's MINUTES format is overridden on purpose.
        val state =
            content(arrivals(row(inTime = 6.minutes), fetchedAt = now - 121.seconds), format = EtaFormat.MINUTES)

        assertEquals(StopUpdatedUi(ageSeconds = 121, isStale = true, needsRefresh = true), state.updated)
        assertEquals(EtaDisplay.ClockTime(hour = 10, minute = 36), state.arrivals.single().eta)
    }

    @Test
    fun lb205_anExpiredListDoesNotSayNow() {
        // Under a minute would be "Now" while the data is fresh; expired data must not claim it
        // (design_guidelines.md §6.3).
        val state =
            content(arrivals(row(inTime = 30.seconds), fetchedAt = now - 300.seconds), format = EtaFormat.MINUTES)

        assertEquals(StopUpdatedUi(ageSeconds = 300, isStale = true, needsRefresh = true), state.updated)
        assertEquals(EtaDisplay.ClockTime(hour = 10, minute = 30), state.arrivals.single().eta)
    }

    @Test
    fun lb205_beforeTheFirstAnswer_thereIsNoAgeToShow() {
        val state = content(arrivals(row(), fetchedAt = null))

        assertNull(state.updated)
    }

    @Test
    fun lb205_aFailedRefreshKeepsTheLastAnswerAndItsAge() {
        // FR-ERR-03 keeps the last good list with lastError set; the age is what says how old what is shown is, since
        // the stop detail's state has no place for the error while there is data to show.
        val state = content(arrivals(row(), fetchedAt = now - 40.seconds, lastError = AppError.Network))

        assertEquals(StopUpdatedUi(ageSeconds = 40, isStale = false, needsRefresh = false), state.updated)
        assertEquals(1, state.arrivals.size)
    }

    @Test
    fun lb205_theUserFormatWordsTheSameRowsAsClockTimes() {
        val rows = StopDetailUiMapper.rows(listOf(row(inTime = 4.minutes)), now, EtaFormat.CLOCK_TIME)

        // FR-SET-01: in the clock-time format every ETA is the Madrid clock time, minutes included.
        assertEquals(EtaDisplay.ClockTime(hour = 10, minute = 34), rows.single().eta)
    }

    @Test
    fun lb205_theAgeIsWholeSeconds_roundedDown() {
        val state = content(arrivals(row(), fetchedAt = now - 50.seconds - 900.milliseconds))

        assertEquals(50, state.updated?.ageSeconds)
    }

    @Test
    fun lb205_anAnswerFromAheadIsNoAgeAtAll() {
        // A time sync between the answer and the render must not print a negative age.
        val state = content(arrivals(row(), fetchedAt = now + 5.seconds))

        assertEquals(StopUpdatedUi(ageSeconds = 0, isStale = false, needsRefresh = false), state.updated)
    }

    @Test
    fun lb205_aFailureIsTheErrorStateWithItsReason() {
        assertEquals(
            StopDetailUiState.Error(ErrorReason.NETWORK),
            StopDetailUiMapper.state(
                AppResult.Failure(AppError.Network),
                stopId = 38,
                now = now,
                format = EtaFormat.MINUTES,
            ),
        )
        assertEquals(
            StopDetailUiState.Error(ErrorReason.SERVER),
            StopDetailUiMapper.state(
                AppResult.Failure(AppError.Server(503)),
                stopId = 38,
                now = now,
                format = EtaFormat.MINUTES,
            ),
        )
    }

    @Test
    fun lb205_anAnswerForAnotherStopIsTheDataError() {
        val other = stop38.copy(id = 40, name = "Portales")

        assertEquals(
            StopDetailUiState.Error(ErrorReason.DATA),
            StopDetailUiMapper.state(
                AppResult.Success(arrivals(row(), stop = other)),
                stopId = 38,
                now = now,
                format = EtaFormat.MINUTES,
            ),
        )
    }
}
