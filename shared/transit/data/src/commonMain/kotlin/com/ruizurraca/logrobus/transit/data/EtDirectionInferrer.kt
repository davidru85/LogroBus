package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.transit.domain.Direction

/** One ET call: the stop it is at (`stopPointRef`, DD-01) and its position in the journey (`order`). */
internal data class EtCall(
    val stopId: Int,
    val order: Int,
)

/**
 * DD-04 and spec §5.6: ET's `directionRef` is an opaque code such as `"401"`, so its direction is inferred from where
 * its calls are. The learned cache of step 1 comes with the ET mapper (LB-006 part 2); this is steps 2 to 5.
 */
internal object EtDirectionInferrer {
    /**
     * The direction of one `(lineRef, directionRef)` pair, from the [calls] that carry it in one response, against
     * the line's [patterns], each its stop ids in order; `null` when they do not say clearly.
     */
    fun infer(
        calls: List<EtCall>,
        patterns: Map<Direction, List<Int>>,
    ): Direction? = calls.firstOrNull()?.let { patterns.keys.firstOrNull() } // TDD: red
}
