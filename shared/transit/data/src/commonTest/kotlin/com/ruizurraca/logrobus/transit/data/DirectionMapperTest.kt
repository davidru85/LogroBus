package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.transit.domain.Direction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DirectionMapperTest {
    @Test
    fun dd04_ascAndIdaAreOutbound() {
        assertEquals(Direction.OUTBOUND, DirectionMapper.fromToken("asc"))
        assertEquals(Direction.OUTBOUND, DirectionMapper.fromToken("Ida"))
    }

    @Test
    fun dd04_descAndVueltaAreInbound() {
        assertEquals(Direction.INBOUND, DirectionMapper.fromToken("desc"))
        assertEquals(Direction.INBOUND, DirectionMapper.fromToken("Vuelta"))
    }

    @Test
    fun dd04_theWordsAreReadWhateverTheirCaseAndSpaces() {
        assertEquals(Direction.OUTBOUND, DirectionMapper.fromToken(" IDA "))
        assertEquals(Direction.INBOUND, DirectionMapper.fromToken("vuelta"))
    }

    @Test
    fun dd04_aMissingEmptyOrUnknownDirectionIsNull() {
        listOf(null, "", " ", "Ida y vuelta", "outbound").forEach { raw ->
            assertNull(DirectionMapper.fromToken(raw), raw)
        }
    }

    @Test
    fun dd04_etNumericCodesAreNotWordsAndAreLeftToTheInference() {
        listOf("401", "402", "321").forEach { raw -> assertNull(DirectionMapper.fromToken(raw), raw) }
    }
}
