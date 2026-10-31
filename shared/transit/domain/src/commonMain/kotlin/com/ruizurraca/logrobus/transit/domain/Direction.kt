package com.ruizurraca.logrobus.transit.domain

/** Spec §5.1: the direction of a line's pattern. `asc` and `Ida` are outbound, `desc` and `Vuelta` inbound (DD-04). */
enum class Direction {
    OUTBOUND,
    INBOUND,
}
