package com.ruizurraca.logrobus.transit.data

import kotlin.time.Instant

/**
 * DD-05: VM's `nextStopAimedArrivalTime` and `nextStopExpectedArrivalTime` are the Madrid wall clock labelled `Z`,
 * while `locationRecordedAtTime` is true UTC. A [raw] next-stop time more than 30 min after [recordedAt] is read again
 * as Madrid time; any other is left alone (risk R4). The vehicle mapper (LB-006 part 2) calls this for both fields.
 */
internal fun correctNextStopTime(
    raw: Instant,
    recordedAt: Instant,
): Instant = maxOf(raw, recordedAt) // TDD: red
