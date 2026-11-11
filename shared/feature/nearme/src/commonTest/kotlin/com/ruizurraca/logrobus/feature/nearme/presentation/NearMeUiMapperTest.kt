package com.ruizurraca.logrobus.feature.nearme.presentation

import com.ruizurraca.logrobus.feature.nearme.domain.LocationAccess
import com.ruizurraca.logrobus.feature.nearme.domain.NearbyStop
import com.ruizurraca.logrobus.feature.nearme.domain.NearbyStops
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

/** FR-NEAR-06: "Near you" without and with location. */
class NearMeUiMapperTest {
    @Test
    fun lb201_withoutLocation_nearYouIsTheTurnOnLocationRow() {
        assertEquals(NearMeUiState(NearYouUi.TurnOnLocation), NearMeUiMapper.state(LocationAccess.Off))
    }

    @Test
    fun lb201_withLocationGrantedButUnavailable_nearYouHasNoRowAskingForIt() {
        assertEquals(NearMeUiState(NearYouUi.LocationUnavailable), NearMeUiMapper.state(LocationAccess.Unavailable))
    }

    @Test
    fun lb201_withLocation_nearYouHasNoRow() {
        assertEquals(NearMeUiState(NearYouUi.LocationOn()), NearMeUiMapper.state(LocationAccess.On))
    }

    @Test
    fun lb203_theStopsReachTheSection_inTheOrderTheUseCaseGaveThem() {
        val nearby =
            NearbyStops(
                stops =
                    listOf(
                        NearbyStop(stopId = 38, name = "Gonzalo de Berceo", distanceMeters = 120, walkingDuration = 90.seconds),
                        NearbyStop(stopId = 61, name = "Varea", distanceMeters = 400),
                    ),
                isRefined = true,
            )

        val section = NearMeUiMapper.state(LocationAccess.On, nearby).nearYou as NearYouUi.LocationOn

        assertEquals(listOf("Gonzalo de Berceo", "Varea"), section.stops.map { it.name })
        assertEquals(listOf(120, 400), section.stops.map { it.distanceMeters })
        assertEquals(2, section.stops.first().walkingMinutes, "90 s of walk rounds up to 2 min, never down to 1")
        assertNull(section.stops.last().walkingMinutes, "a stop API-08 did not answer for has no walk to show")
    }

    @Test
    fun lb203_aWalkUnderAMinuteStillReadsAsOne() {
        val nearby = NearbyStops(listOf(NearbyStop(stopId = 1, name = "Casa", distanceMeters = 20, walkingDuration = 40.seconds)))

        val section = NearMeUiMapper.state(LocationAccess.On, nearby).nearYou as NearYouUi.LocationOn

        assertEquals(1, section.stops.single().walkingMinutes, "40 s never reads 0 min")
    }

    @Test
    fun lb203_untilTheFirstPositionTheSectionIsEmpty_notAbsent() {
        val section = NearMeUiMapper.state(LocationAccess.On).nearYou as NearYouUi.LocationOn

        assertEquals(emptyList(), section.stops)
        assertEquals(false, section.isRefined)
    }
}
