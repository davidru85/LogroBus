package com.ruizurraca.logrobus.feature.planner.presentation

import com.ruizurraca.logrobus.feature.planner.domain.PlanDeparture
import com.ruizurraca.logrobus.feature.planner.domain.PlanPlace
import com.ruizurraca.logrobus.feature.planner.domain.PlanPlaces
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** FR-PLAN-11 and FR-PLAN-08: How to Get There's fields, each showing its own end of the trip. */
class PlannerUiMapperTest {
    // Leaving now, with the pickers closed: the fields alone.
    private fun state(places: PlanPlaces) =
        PlannerUiMapper.state(places, PlanDeparture.Now, picking = false, now = Instant.parse("2026-10-22T05:00:00Z"))

    @Test
    fun lb301_withMyLocation_theOriginShowsMyLocation_andTheDestinationIsEmpty() {
        val state = state(PlanPlaces(origin = PlanPlace.MyLocation))

        assertEquals(PlannerUiState(origin = PlaceFieldUi.MyLocation, destination = PlaceFieldUi.Empty), state)
    }

    @Test
    fun lb301_withoutAnOrigin_bothFieldsAreEmpty() {
        val state = state(PlanPlaces())

        assertEquals(PlannerUiState(origin = PlaceFieldUi.Empty, destination = PlaceFieldUi.Empty), state)
    }

    @Test
    fun lb304_eachFieldShowsItsOwnEnd_myLocationAsTheDestinationAfterASwap() {
        val state = state(PlanPlaces(origin = null, destination = PlanPlace.MyLocation))

        assertEquals(PlannerUiState(origin = PlaceFieldUi.Empty, destination = PlaceFieldUi.MyLocation), state)
    }

    @Test
    fun lb303_aPickedTimeIsReadOnTheMadridClock() {
        // FR-PLAN-03: the pickers' fields are Madrid wall-clock values, so the instant is theirs in that zone, not
        // UTC. The event is the screen's own door to that conversion.
        val departure =
            PlannerEvent
                .DepartAt(year = 2026, month = 12, day = 15, hour = 18, minute = 30)
                .departure(now = Instant.parse("2026-12-01T09:00:00Z"))

        assertEquals(Instant.parse("2026-12-15T17:30:00Z"), departure.instant, "CET is one hour ahead in December")
    }
}
