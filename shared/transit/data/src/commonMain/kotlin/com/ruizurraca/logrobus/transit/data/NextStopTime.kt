package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.common.madridTimeZone
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

// DD-05: no real next stop is this far ahead of a report labelled in the same time zone.
private val MISLABELLED_AFTER = 30.minutes

// Wider than any offset change around a wall-clock time, so the offsets on both sides of it are found.
private val AROUND = 3.hours

/**
 * DD-05: VM's `nextStopAimedArrivalTime` and `nextStopExpectedArrivalTime` are the Madrid wall clock labelled `Z`,
 * while `locationRecordedAtTime` is true UTC. A [raw] next-stop time more than 30 min after [recordedAt] is read again
 * as Madrid time; any other is left alone (risk R4). The vehicle mapper (LB-006 part 2) calls this for both fields.
 */
internal fun correctNextStopTime(
    raw: Instant,
    recordedAt: Instant,
): Instant {
    if (raw - recordedAt <= MISLABELLED_AFTER) return raw
    val wallClock = raw.toLocalDateTime(TimeZone.UTC)
    return madridReadings(wallClock).minBy { (it - recordedAt).absoluteValue }
}

/**
 * Every instant at which Madrid's clock shows [wallClock]: two in the hour repeated on the last Sunday of October, so
 * the caller picks the one nearer the report, and one at any other time. A time in the hour skipped in March shows on
 * no clock; the time zone's own reading, an hour later, stands for it.
 */
private fun madridReadings(wallClock: LocalDateTime): List<Instant> {
    val usual = wallClock.toInstant(madridTimeZone)
    val offsets = setOf(madridTimeZone.offsetAt(usual - AROUND), madridTimeZone.offsetAt(usual + AROUND))
    return offsets
        .map { wallClock.toInstant(it) }
        .filter { it.toLocalDateTime(madridTimeZone) == wallClock }
        .ifEmpty { listOf(usual) }
}
