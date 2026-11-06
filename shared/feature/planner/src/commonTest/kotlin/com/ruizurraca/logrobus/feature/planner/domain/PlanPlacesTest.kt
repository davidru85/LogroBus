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
    fun lb301_withoutThePermission_myLocationLeavesTheOrigin() {
        val asOrigin = PlanPlaces(origin = PlanPlace.MyLocation, destination = null)

        assertEquals(PlanPlaces(), asOrigin.following(initialOrigin = null))
    }

    @Test
    fun lb304_withoutThePermission_myLocationLeavesTheDestinationToo() {
        val swapped = PlanPlaces(origin = null, destination = PlanPlace.MyLocation)

        assertEquals(PlanPlaces(), swapped.following(initialOrigin = null))
    }

    @Test
    fun lb304_afterASwap_thePermissionRevokedThenGrantedAgain_startsTheTripFromMyLocationAgain() {
        // My location left the destination with the permission, and comes back as a new plan's origin.
        val swapped = PlanPlaces(origin = null, destination = PlanPlace.MyLocation)

        val revoked = swapped.following(initialOrigin = null)
        val granted = revoked.following(initialOrigin = PlanPlace.MyLocation)

        assertEquals(PlanPlaces(origin = PlanPlace.MyLocation, destination = null), granted)
    }
}
