package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.transit.domain.Direction

/** DD-04: the words for a direction. ET's numeric codes are not words; `EtDirectionInferrer` resolves those. */
internal object DirectionMapper {
    fun fromToken(raw: String?): Direction? = raw?.let { Direction.OUTBOUND } // TDD: red
}
