package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.transit.domain.Direction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Spec §5.6, steps 2 to 5. Patterns are stop ids in order; a call's `order` may count from 0 or from 1. */
class EtDirectionInferrerTest {
    private val outbound = (1..10).toList()
    private val inbound = outbound.reversed()
    private val line = mapOf(Direction.OUTBOUND to outbound, Direction.INBOUND to inbound)

    @Test
    fun dd04_callsThatFollowOnePatternTakeItsDirection() {
        val forwards = outbound.mapIndexed { index, stop -> EtCall(stop, order = index + 1) }
        val backwards = inbound.mapIndexed { index, stop -> EtCall(stop, order = index + 1) }

        assertEquals(Direction.OUTBOUND, EtDirectionInferrer.infer(forwards, line))
        assertEquals(Direction.INBOUND, EtDirectionInferrer.infer(backwards, line))
    }

    @Test
    fun dd04_anOrderCountedFromZeroMatchesToo() {
        val calls = inbound.take(6).mapIndexed { index, stop -> EtCall(stop, order = index) }

        assertEquals(Direction.INBOUND, EtDirectionInferrer.infer(calls, line))
    }

    @Test
    fun dd04_eightyPercentForOnePatternAndUnderFiftyForTheOtherDecides() {
        // Eight of ten calls on the outbound pattern; the first four also sit where the inbound pattern has them.
        val calls = (1..8).map { EtCall(it, order = it) } + listOf(EtCall(99, order = 9), EtCall(98, order = 10))
        val inboundSharingFour = listOf(1, 2, 3, 4) + (60..65)
        val inboundSharingFive = listOf(1, 2, 3, 4, 5) + (60..64)

        assertEquals(
            Direction.OUTBOUND,
            EtDirectionInferrer.infer(calls, line + (Direction.INBOUND to inboundSharingFour)),
        )
        assertNull(EtDirectionInferrer.infer(calls, line + (Direction.INBOUND to inboundSharingFive)))
    }

    @Test
    fun dd04_underEightyPercentForEitherPatternIsNull() {
        val calls = (1..7).map { EtCall(it, order = it) } + (1..3).map { EtCall(90 + it, order = 7 + it) }

        assertNull(EtDirectionInferrer.infer(calls, line))
    }

    @Test
    fun dd04_callsThatFitBothPatternsAreNull() {
        val calls = (1..5).map { EtCall(it, order = it) }
        val both = mapOf(Direction.OUTBOUND to outbound, Direction.INBOUND to outbound)

        assertNull(EtDirectionInferrer.infer(calls, both))
    }

    @Test
    fun dd04_callsAtOneStopOfOnlyOnePatternTakeItsDirection() {
        // Stop 11 is only on the inbound pattern. One stop says nothing by its position, only by its membership.
        val inboundWithItsOwnStop = listOf(11) + inbound
        val calls = listOf(EtCall(11, order = 7), EtCall(11, order = 7))

        assertEquals(
            Direction.INBOUND,
            EtDirectionInferrer.infer(calls, line + (Direction.INBOUND to inboundWithItsOwnStop)),
        )
    }

    @Test
    fun dd04_callsAtOneStopOfBothPatternsAreNull() {
        // Stop 3 is third outbound, which this order fits; but one stop on both patterns cannot tell them apart.
        assertNull(EtDirectionInferrer.infer(listOf(EtCall(3, order = 3)), line))
    }

    @Test
    fun dd04_noCallsOrNoPatternsIsNull() {
        assertNull(EtDirectionInferrer.infer(emptyList(), line))
        assertNull(EtDirectionInferrer.infer(listOf(EtCall(1, order = 1), EtCall(2, order = 2)), emptyMap()))
    }
}
