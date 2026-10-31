package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.transit.domain.Direction

/** One ET call: the stop it is at (`stopPointRef`, DD-01) and its position in the journey (`order`). */
internal data class EtCall(
    val stopId: Int,
    val order: Int,
)

/** A direction found by [EtDirectionInferrer], and whether spec §5.6 lets the learned cache keep it. */
internal data class InferredDirection(
    val direction: Direction,
    val cacheable: Boolean,
)

/**
 * DD-04 and spec §5.6: ET's `directionRef` is an opaque code such as `"401"`, so its direction is inferred from where
 * its calls are. The learned cache of step 1 comes with the ET mapper (LB-006 part 2); this is steps 2 to 5.
 */
internal object EtDirectionInferrer {
    private const val PERCENT = 100

    // Step 3: one pattern must hold at least this share of the calls, and every other less than the doubtful one.
    private const val DECIDING_PERCENT = 80
    private const val DOUBTFUL_PERCENT = 50

    // Step 2: a call fits a pattern that has its stop within one place of its order.
    private const val TOLERANCE = 1

    /**
     * The direction of one `(lineRef, directionRef)` pair, from the [calls] that carry it in one response, against
     * the line's [patterns], each its stop ids in order; `null` when they do not say clearly.
     */
    fun infer(
        calls: List<EtCall>,
        patterns: Map<Direction, List<Int>>,
    ): InferredDirection? {
        if (calls.isEmpty()) return null
        // API-06 answers with calls at one stop. Their order still places them, but they are one observation, so the
        // cache, which step 3 fills, never keeps what they decide.
        val oneStop = calls.all { it.stopId == calls.first().stopId }
        val placed = byPosition(calls, patterns)
        return when {
            placed != null -> InferredDirection(placed, cacheable = !oneStop)
            oneStop -> byMembership(calls.first().stopId, patterns)?.let { InferredDirection(it, cacheable = false) }
            else -> null
        }
    }

    // Steps 2 and 3. Whole percentages round down, which keeps both comparisons exact.
    private fun byPosition(
        calls: List<EtCall>,
        patterns: Map<Direction, List<Int>>,
    ): Direction? {
        val shares = patterns.mapValues { (_, stops) -> calls.count { it.fits(stops) } * PERCENT / calls.size }
        val decided = shares.filterValues { it >= DECIDING_PERCENT }.keys.singleOrNull()
        return decided?.takeIf { shares.all { (direction, share) -> direction == decided || share < DOUBTFUL_PERCENT } }
    }

    // Step 4: when its order fits both patterns or neither, a stop on only one pattern still fixes the direction.
    private fun byMembership(
        stopId: Int,
        patterns: Map<Direction, List<Int>>,
    ): Direction? = patterns.filterValues { stopId in it }.keys.singleOrNull()

    // Order counts from 1, as spec §4.6's example shows: stop 38, the 25th and last of line 1 outbound, at order 25.
    private fun EtCall.fits(stops: List<Int>): Boolean {
        val index = order - 1
        return (index - TOLERANCE..index + TOLERANCE).any { stops.getOrNull(it) == stopId }
    }
}
