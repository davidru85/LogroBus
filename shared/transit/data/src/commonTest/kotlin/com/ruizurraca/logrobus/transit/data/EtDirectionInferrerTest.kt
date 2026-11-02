package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.transit.domain.Direction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Spec §5.6, steps 2 to 5. Patterns are stop ids in order. A call's `order` counts from 1, as spec §4.6's example
 * suggests (stop 38, the 25th and last of line 1 outbound, at order 25); a call fits within one place of it.
 */
class EtDirectionInferrerTest {
    private val outbound = (1..10).toList()
    private val inbound = outbound.reversed()
    private val line = mapOf(Direction.OUTBOUND to outbound, Direction.INBOUND to inbound)

    @Test
    fun dd04_callsThatFollowOnePatternTakeItsDirection() {
        val forwards = outbound.mapIndexed { index, stop -> EtCall(stop, order = index + 1) }
        val backwards = inbound.mapIndexed { index, stop -> EtCall(stop, order = index + 1) }

        assertEquals(InferredDirection(Direction.OUTBOUND, cacheable = true), EtDirectionInferrer.infer(forwards, line))
        assertEquals(InferredDirection(Direction.INBOUND, cacheable = true), EtDirectionInferrer.infer(backwards, line))
    }

    @Test
    fun dd04_anOrderOneAboveOrBelowItsPositionStillFits() {
        val oneAbove = outbound.mapIndexed { index, stop -> EtCall(stop, order = index + 2) }
        val oneBelow = outbound.mapIndexed { index, stop -> EtCall(stop, order = index) }

        assertEquals(Direction.OUTBOUND, EtDirectionInferrer.infer(oneAbove, line)?.direction)
        assertEquals(Direction.OUTBOUND, EtDirectionInferrer.infer(oneBelow, line)?.direction)
    }

    @Test
    fun dd04_anOrderTwoAwayFromItsPositionDoesNotFit() {
        val twoAbove = outbound.mapIndexed { index, stop -> EtCall(stop, order = index + 3) }
        val twoBelow = outbound.mapIndexed { index, stop -> EtCall(stop, order = index - 1) }

        assertNull(EtDirectionInferrer.infer(twoAbove, line))
        assertNull(EtDirectionInferrer.infer(twoBelow, line))
    }

    @Test
    fun dd04_eightyPercentForOnePatternAndUnderFiftyForTheOtherDecides() {
        // Eight of ten calls on the outbound pattern; the first four also sit where the inbound pattern has them.
        val calls = (1..8).map { EtCall(it, order = it) } + listOf(EtCall(99, order = 9), EtCall(98, order = 10))
        val inboundSharingFour = listOf(1, 2, 3, 4) + (60..65)
        val inboundSharingFive = listOf(1, 2, 3, 4, 5) + (60..64)

        assertEquals(
            Direction.OUTBOUND,
            EtDirectionInferrer.infer(calls, line + (Direction.INBOUND to inboundSharingFour))?.direction,
        )
        assertNull(EtDirectionInferrer.infer(calls, line + (Direction.INBOUND to inboundSharingFive)))
    }

    @Test
    fun dd04_aShareJustUnderEightyPercentDoesNotDecide() {
        val long = mapOf(Direction.OUTBOUND to (1..49).toList(), Direction.INBOUND to (101..149).toList())

        fun calls(fitting: Int) =
            (1..fitting).map { EtCall(it, order = it) } + (fitting + 1..49).map { EtCall(200 + it, order = it) }

        // 39 of 49 is 79.6 %; 40 of 49 is 81.6 %.
        assertNull(EtDirectionInferrer.infer(calls(fitting = 39), long))
        assertEquals(Direction.OUTBOUND, EtDirectionInferrer.infer(calls(fitting = 40), long)?.direction)
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
    fun dd04_oneStopAtDifferentPlacesOnThePatternsIsDecidedByItsOrderButNotCached() {
        // Stop 3 is third outbound and eighth inbound; API-06 answers with calls at one stop like this one.
        assertEquals(
            InferredDirection(Direction.OUTBOUND, cacheable = false),
            EtDirectionInferrer.infer(listOf(EtCall(3, order = 3), EtCall(3, order = 3)), line),
        )
    }

    @Test
    fun dd04_oneStopOfOnlyOnePatternTakesItsDirectionButIsNotCached() {
        // Stop 11 is only on the inbound pattern, at a place its order does not fit.
        val inboundWithItsOwnStop = listOf(11) + inbound

        assertEquals(
            InferredDirection(Direction.INBOUND, cacheable = false),
            EtDirectionInferrer.infer(
                listOf(EtCall(11, order = 7)),
                line + (Direction.INBOUND to inboundWithItsOwnStop),
            ),
        )
    }

    @Test
    fun dd04_oneStopAtTheSamePlaceOnBothPatternsIsNull() {
        // Stop 5 is fifth outbound and sixth inbound: its order fits both.
        assertNull(EtDirectionInferrer.infer(listOf(EtCall(5, order = 5)), line))
    }

    @Test
    fun dd04_noCallsOrNoPatternsIsNull() {
        assertNull(EtDirectionInferrer.infer(emptyList(), line))
        assertNull(EtDirectionInferrer.infer(listOf(EtCall(1, order = 1), EtCall(2, order = 2)), emptyMap()))
    }
}
