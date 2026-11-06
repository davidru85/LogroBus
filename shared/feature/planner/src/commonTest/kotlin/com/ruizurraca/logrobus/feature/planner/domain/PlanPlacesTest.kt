package com.ruizurraca.logrobus.feature.planner.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * FR-PLAN-08: one swap exchanges the trip's ends. FR-PLAN-11: each time the root shows, "My location" follows the
 * permission, without undoing a swap.
 */
class PlanPlacesTest {
    @Test
    fun lb304_aSwap_makesTheOriginTheDestination_andTheDestinationTheOrigin() {
        val places = PlanPlaces(origin = PlanPlace.MyLocation, destination = null)

        assertEquals(PlanPlaces(origin = null, destination = PlanPlace.MyLocation), places.swapped())
    }

    @Test
    fun lb304_aSecondSwap_restoresTheTrip() {
        val places = PlanPlaces(origin = PlanPlace.MyLocation, destination = null)

        assertEquals(places, places.swapped().swapped())
    }

    @Test
    fun lb304_afterASwap_theRootShowingAgain_keepsMyLocationAsTheDestination() {
        val swapped = PlanPlaces(origin = null, destination = PlanPlace.MyLocation)

        assertEquals(swapped, swapped.following(initialOrigin = PlanPlace.MyLocation))
    }

    @Test
    fun lb301_withThePermission_myLocationFillsAnEmptyOrigin() {
        assertEquals(
            PlanPlaces(origin = PlanPlace.MyLocation, destination = null),
            PlanPlaces().following(initialOrigin = PlanPlace.MyLocation),
        )
    }

    @Test
    fun lb301_withoutThePermission_myLocationLeavesWhicheverFieldHoldsIt() {
        val asOrigin = PlanPlaces(origin = PlanPlace.MyLocation, destination = null)
        val asDestination = PlanPlaces(origin = null, destination = PlanPlace.MyLocation)

        assertEquals(PlanPlaces(), asOrigin.following(initialOrigin = null))
        assertEquals(PlanPlaces(), asDestination.following(initialOrigin = null))
    }
}
