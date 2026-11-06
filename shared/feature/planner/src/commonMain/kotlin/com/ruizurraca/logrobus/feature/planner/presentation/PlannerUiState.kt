package com.ruizurraca.logrobus.feature.planner.presentation

import com.ruizurraca.logrobus.core.common.madridTimeZone
import com.ruizurraca.logrobus.core.presentation.MadridDateTime
import com.ruizurraca.logrobus.feature.planner.domain.PlanDeparture
import com.ruizurraca.logrobus.feature.planner.domain.PlanPlace
import com.ruizurraca.logrobus.feature.planner.domain.PlanPlaces
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * How to Get There's root (design_guidelines.md §3.3). Until place search lands (LB-301 part 2), its origin and
 * destination fields (owner decision, 2026-11-06), and the departure chip with its pickers (FR-PLAN-03).
 */
data class PlannerUiState(
    val origin: PlaceFieldUi = PlaceFieldUi.Empty,
    val destination: PlaceFieldUi = PlaceFieldUi.Empty,
    val departure: DepartureUi = DepartureUi.Now,
    /** The date and time pickers, while the user chooses a departure time; null while they are closed. */
    val departurePicker: DeparturePickerUi? = null,
)

/** What an origin or destination field shows. */
sealed interface PlaceFieldUi {
    /** Nothing chosen: the field shows its hint, "Where from?" or "Where to?". */
    data object Empty : PlaceFieldUi

    /** The user's location: "My location" (FR-PLAN-11). */
    data object MyLocation : PlaceFieldUi
}

/** What the departure chip says (FR-PLAN-03). */
sealed interface DepartureUi {
    /** "Leave now". */
    data object Now : DepartureUi

    /**
     * "Depart at" [time], on the Madrid clock. [relativeDay] counts from today there: "Depart at 08:19", "Depart
     * tomorrow at 08:19", or "Depart on 22 Oct at 08:19".
     */
    data class At(
        val time: MadridDateTime,
        val relativeDay: DepartureDay,
    ) : DepartureUi
}

/**
 * A departure's day, on the Madrid calendar: today, tomorrow, or another day, which shows its date. A day gone by is
 * another day too: the root keeps a picked time until the user changes it (LB-303).
 */
enum class DepartureDay { TODAY, TOMORROW, OTHER }

/**
 * Where the date and time pickers open, on the Madrid clock: at [start], the picked time, or now for "Leave now" or
 * a picked time gone by. [now] is the Madrid clock's when they opened: the date picker offers no day before its own.
 */
data class DeparturePickerUi(
    val start: MadridDateTime,
    val now: MadridDateTime,
)

/** What How to Get There's root sends its ViewModel. */
sealed interface PlannerEvent {
    /**
     * The root shows: opened, selected again, or back from the background. The location permission may have changed
     * meanwhile, in Near Me or in the system's settings, so the initial origin is read again (FR-PLAN-11).
     */
    data object Shown : PlannerEvent

    /** The swap button: the origin becomes the destination, and the destination the origin (FR-PLAN-08). */
    data object Swap : PlannerEvent

    /** The departure menu's "Leave now" (FR-PLAN-03). */
    data object LeaveNow : PlannerEvent

    /** The departure menu's "Choose a time": the date and time pickers open (FR-PLAN-03). */
    data object ChooseDepartureTime : PlannerEvent

    /** The pickers closed without a time: the departure stays as it was. */
    data object CancelDepartureTime : PlannerEvent

    /** A date and time picked on the Madrid clock (AGENTS.md §5.5): the trip departs at it, and the pickers close. */
    data class DepartAt(
        val year: Int,
        val month: Int,
        val day: Int,
        val hour: Int,
        val minute: Int,
    ) : PlannerEvent
}

object PlannerUiMapper {
    /**
     * The root for [places] and [departure], with the pickers open while [picking]. Each field shows its own end,
     * "My location" in either (FR-PLAN-08). [now] is the app clock's (spec §8.6), on which the departure's day and the
     * pickers' start depend.
     */
    fun state(
        places: PlanPlaces,
        departure: PlanDeparture,
        picking: Boolean,
        now: Instant,
    ): PlannerUiState =
        PlannerUiState(
            origin = field(places.origin),
            destination = field(places.destination),
            departure = departure(departure, now),
            // At the picked moment, or at now for "Leave now" or a moment gone by, which the pickers no longer offer.
            departurePicker = if (picking) picker(start = maxOf(departure.departureAt(now), now), now = now) else null,
        )

    private fun picker(
        start: Instant,
        now: Instant,
    ) = DeparturePickerUi(start = MadridDateTime.of(start), now = MadridDateTime.of(now))

    private fun departure(
        departure: PlanDeparture,
        now: Instant,
    ): DepartureUi =
        when (departure) {
            PlanDeparture.Now -> DepartureUi.Now
            is PlanDeparture.At -> DepartureUi.At(MadridDateTime.of(departure.instant), day(departure.instant, now))
        }

    // Days on the Madrid calendar: ten minutes after 23:50 is tomorrow.
    private fun day(
        instant: Instant,
        now: Instant,
    ): DepartureDay {
        val today = now.madridDate()
        return when (instant.madridDate()) {
            today -> DepartureDay.TODAY
            today.plus(1, DateTimeUnit.DAY) -> DepartureDay.TOMORROW
            else -> DepartureDay.OTHER
        }
    }

    private fun Instant.madridDate(): LocalDate = toLocalDateTime(madridTimeZone).date

    private fun field(place: PlanPlace?): PlaceFieldUi =
        when (place) {
            null -> PlaceFieldUi.Empty
            PlanPlace.MyLocation -> PlaceFieldUi.MyLocation
        }
}
