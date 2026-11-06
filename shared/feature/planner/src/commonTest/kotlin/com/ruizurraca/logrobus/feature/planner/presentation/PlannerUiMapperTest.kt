package com.ruizurraca.logrobus.feature.planner.presentation

import com.ruizurraca.logrobus.feature.planner.domain.PlanPlace
import com.ruizurraca.logrobus.feature.planner.domain.PlanPlaces
import kotlin.test.Test
import kotlin.test.assertEquals

/** FR-PLAN-11: How to Get There's fields, with and without the user's location as the origin. */
class PlannerUiMapperTest {
    @Test
    fun lb301_withMyLocation_theOriginShowsMyLocation_andTheDestinationIsEmpty() {
        val state = PlannerUiMapper.state(origin = PlanPlace.MyLocation)

        assertEquals(PlannerUiState(origin = PlaceFieldUi.MyLocation, destination = PlaceFieldUi.Empty), state)
    }

    @Test
    fun lb301_withoutAnOrigin_bothFieldsAreEmpty() {
        val state = PlannerUiMapper.state(origin = null)

        assertEquals(PlannerUiState(origin = PlaceFieldUi.Empty, destination = PlaceFieldUi.Empty), state)
    }

    @Test
    fun lb304_eachFieldShowsItsOwnEnd_myLocationAsTheDestinationAfterASwap() {
        val state = PlannerUiMapper.state(PlanPlaces(origin = null, destination = PlanPlace.MyLocation))

        assertEquals(PlannerUiState(origin = PlaceFieldUi.Empty, destination = PlaceFieldUi.MyLocation), state)
    }

    @Test
    fun lb304_myLocationAsTheOrigin_showsInTheOriginField() {
        val state = PlannerUiMapper.state(PlanPlaces(origin = PlanPlace.MyLocation))

        assertEquals(PlannerUiState(origin = PlaceFieldUi.MyLocation, destination = PlaceFieldUi.Empty), state)
    }
}
