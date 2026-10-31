package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.network.trimmedOrNull
import com.ruizurraca.logrobus.transit.domain.Direction

/** DD-04: the words for a direction. ET's numeric codes are not words; `EtDirectionInferrer` resolves those. */
internal object DirectionMapper {
    // API-01's pattern keys, and VM's directionRef.
    private val words =
        mapOf(
            "asc" to Direction.OUTBOUND,
            "ida" to Direction.OUTBOUND,
            "desc" to Direction.INBOUND,
            "vuelta" to Direction.INBOUND,
        )

    fun fromToken(raw: String?): Direction? = raw.trimmedOrNull()?.lowercase()?.let(words::get)
}
