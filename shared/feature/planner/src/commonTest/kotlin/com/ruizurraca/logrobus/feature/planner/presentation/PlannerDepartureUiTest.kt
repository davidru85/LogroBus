package com.ruizurraca.logrobus.feature.planner.presentation

import com.ruizurraca.logrobus.core.presentation.MadridDateTime
import com.ruizurraca.logrobus.feature.planner.domain.PlanDeparture
import com.ruizurraca.logrobus.feature.planner.domain.PlanPlaces
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

/**
 * FR-PLAN-03 on How to Get There's root: what the departure chip says, and where its date and time pickers open, on
 * the Madrid clock (AGENTS.md §5.5).
 */
class PlannerDepartureUiTest {
    // 07:00 on Thursday 22 Oct 2026 in Madrid (CEST).
    private val now = Instant.parse("2026-10-22T05:00:00Z")

    private fun state(
        departure: PlanDeparture,
        picking: Boolean = false,
        now: Instant = this.now,
    ) = PlannerUiMapper.state(PlanPlaces(), departure, picking, now)

    private fun dayOf(
        departure: PlanDeparture,
        now: Instant,
    ): DepartureDay? = (state(departure, now = now).departure as? DepartureUi.At)?.relativeDay

    @Test
    fun lb303_leaveNow_saysLeaveNow() {
        assertEquals(DepartureUi.Now, state(PlanDeparture.Now).departure)
    }

    @Test
    fun lb303_departAtATimeToday_saysTheTime() {
        val departure = PlanDeparture.atMadrid(LocalDateTime(2026, 10, 22, 8, 19), now)

        assertEquals(
            DepartureUi.At(MadridDateTime(2026, 10, 22, 8, 19), DepartureDay.TODAY),
            state(departure).departure,
        )
    }

    @Test
    fun lb303_departAtATimeTomorrow_saysTomorrow() {
        val departure = PlanDeparture.atMadrid(LocalDateTime(2026, 10, 23, 8, 19), now)

        assertEquals(
            DepartureUi.At(MadridDateTime(2026, 10, 23, 8, 19), DepartureDay.TOMORROW),
            state(departure).departure,
        )
    }

    @Test
    fun lb303_departAtATimeOnAnotherDay_saysItsDate() {
        val departure = PlanDeparture.atMadrid(LocalDateTime(2027, 1, 15, 8, 19), now)

        assertEquals(
            DepartureUi.At(MadridDateTime(2027, 1, 15, 8, 19), DepartureDay.OTHER),
            state(departure).departure,
        )
    }

    @Test
    fun lb303_todayAndTomorrow_areDaysOnTheMadridClock() {
        // 22:30 UTC on 22 Oct is 00:30 on 23 Oct in Madrid: 08:19 on 23 Oct is today there, though tomorrow in UTC.
        val afterMidnightInMadrid = Instant.parse("2026-10-22T22:30:00Z")
        val departure = PlanDeparture.atMadrid(LocalDateTime(2026, 10, 23, 8, 19), now)

        assertEquals(DepartureDay.TODAY, dayOf(departure, now = afterMidnightInMadrid))
    }

    @Test
    fun lb303_tomorrow_isTheNextDay_evenMinutesAway() {
        // 23:50 on 22 Oct in Madrid; 00:10 on 23 Oct is twenty minutes away, but tomorrow.
        val beforeMidnight = Instant.parse("2026-10-22T21:50:00Z")
        val departure = PlanDeparture.atMadrid(LocalDateTime(2026, 10, 23, 0, 10), now)

        assertEquals(DepartureDay.TOMORROW, dayOf(departure, now = beforeMidnight))
    }

    @Test
    fun lb303_todayAndTomorrow_areDaysOnTheMadridClock_inWinterTime() {
        // 23:30 UTC on 6 Nov is 00:30 on 7 Nov in Madrid (CET): 08:19 on 7 Nov is today there.
        val afterMidnightInMadrid = Instant.parse("2026-11-06T23:30:00Z")
        val departure = PlanDeparture.atMadrid(LocalDateTime(2026, 11, 7, 8, 19), now)

        assertEquals(DepartureDay.TODAY, dayOf(departure, now = afterMidnightInMadrid))
    }

    @Test
    fun lb303_tomorrow_isTheNextDay_onTheLastSundayOfOctober() {
        // 23:30 CET on Sunday 25 Oct 2026, a 25-hour day: 00:10 on Monday is tomorrow, though the same day in UTC.
        val sundayNight = Instant.parse("2026-10-25T22:30:00Z")
        val departure = PlanDeparture.atMadrid(LocalDateTime(2026, 10, 26, 0, 10), now)

        assertEquals(DepartureDay.TOMORROW, dayOf(departure, now = sundayNight))
    }

    @Test
    fun lb303_tomorrow_isTheNextDay_onTheLastSundayOfMarch() {
        // 23:30 CEST on Sunday 29 Mar 2026, a 23-hour day: 00:10 on Monday is tomorrow, though the same day in UTC.
        val sundayNight = Instant.parse("2026-03-29T21:30:00Z")
        val departure = PlanDeparture.atMadrid(LocalDateTime(2026, 3, 30, 0, 10), now)

        assertEquals(DepartureDay.TOMORROW, dayOf(departure, now = sundayNight))
    }

    @Test
    fun lb303_aDepartureOnADayGoneBy_showsItsDate() {
        // Kept from three days before, as the root keeps a picked time.
        val departure = PlanDeparture.atMadrid(LocalDateTime(2026, 10, 19, 8, 19), now)

        assertEquals(DepartureDay.OTHER, dayOf(departure, now = now))
    }

    @Test
    fun lb303_thePickersAreClosed_untilATimeIsToBeChosen() {
        assertNull(state(PlanDeparture.Now).departurePicker)
    }

    @Test
    fun lb303_forLeaveNow_thePickersOpenAtNow() {
        val now = MadridDateTime(2026, 10, 22, 7, 0)
        val picker = state(PlanDeparture.Now, picking = true).departurePicker

        assertEquals(DeparturePickerUi(start = now, now = now), picker)
    }

    @Test
    fun lb303_forDepartAt_thePickersOpenAtThePickedTime() {
        val departure = PlanDeparture.atMadrid(LocalDateTime(2026, 10, 23, 8, 19), now)

        assertEquals(
            DeparturePickerUi(start = MadridDateTime(2026, 10, 23, 8, 19), now = MadridDateTime(2026, 10, 22, 7, 0)),
            state(departure, picking = true).departurePicker,
        )
    }

    @Test
    fun lb303_forATimeGoneBy_thePickersOpenAtNow() {
        // Kept from three days before, as the root keeps a picked time: neither its day nor its hour is offered now.
        val departure = PlanDeparture.atMadrid(LocalDateTime(2026, 10, 19, 8, 19), now)
        val now = MadridDateTime(2026, 10, 22, 7, 0)

        assertEquals(DeparturePickerUi(start = now, now = now), state(departure, picking = true).departurePicker)
    }

    @Test
    fun lb303_forATimeEarlierToday_thePickersOpenAtNow() {
        val departure = PlanDeparture.atMadrid(LocalDateTime(2026, 10, 22, 6, 30), now)
        val now = MadridDateTime(2026, 10, 22, 7, 0)

        assertEquals(DeparturePickerUi(start = now, now = now), state(departure, picking = true).departurePicker)
    }
}
