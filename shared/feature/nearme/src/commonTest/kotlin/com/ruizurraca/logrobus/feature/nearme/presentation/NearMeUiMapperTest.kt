package com.ruizurraca.logrobus.feature.nearme.presentation

import com.ruizurraca.logrobus.feature.nearme.domain.LocationAccess
import kotlin.test.Test
import kotlin.test.assertEquals

/** FR-NEAR-06: "Near you" without and with location. */
class NearMeUiMapperTest {
    @Test
    fun lb201_withoutLocation_nearYouIsTheTurnOnLocationRow() {
        assertEquals(NearMeUiState(NearYouUi.TurnOnLocation), NearMeUiMapper.state(LocationAccess.Off))
    }

    @Test
    fun lb201_withLocation_nearYouHasNoRow() {
        assertEquals(NearMeUiState(NearYouUi.LocationOn), NearMeUiMapper.state(LocationAccess.On))
    }
}
