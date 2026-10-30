package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.transit.domain.Direction
import com.ruizurraca.logrobus.transit.domain.LineKind

/** A direction's key in the database. Stored on devices, so it never changes with the enum's names. */
internal val Direction.key: String
    get() =
        when (this) {
            Direction.OUTBOUND -> "outbound"
            Direction.INBOUND -> "inbound"
        }

/** A line kind's key in the database. Stored on devices, so it never changes with the enum's names. */
internal val LineKind.key: String
    get() =
        when (this) {
            LineKind.DAY -> "day"
            LineKind.NIGHT -> "night"
        }
