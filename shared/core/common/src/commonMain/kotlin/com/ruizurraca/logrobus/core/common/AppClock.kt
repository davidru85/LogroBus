package com.ruizurraca.logrobus.core.common

import kotlin.concurrent.Volatile
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/** The only source of "now" for business logic (AGENTS.md §5.5): never call the device clock directly. */
fun interface AppClock {
    fun now(): Instant
}

/** Spec §8.6: how many recent `Date` samples the server offset is smoothed over. */
private const val OFFSET_SAMPLES = 5

/**
 * The device clock corrected by the server offset (spec §8.6, FR-ERR-06). The network layer passes every BFF
 * response's `Date` header to [onServerDate]. The offset is the median of the last [OFFSET_SAMPLES] samples: one
 * stale `Date`, such as a cached response's, does not move it. Until the first sample arrives, the offset is zero.
 */
class ServerCorrectedClock(
    private val deviceClock: Clock,
) : AppClock {
    // Replaced, never mutated. Two responses that land at once can lose one sample, which the median absorbs.
    @Volatile
    private var offsets: List<Duration> = emptyList()

    /** The median of the recent `serverDate - deviceNow` samples. */
    val serverOffset: Duration
        get() = offsets.median()

    fun onServerDate(serverDate: Instant) {
        offsets = (offsets + (serverDate - deviceClock.now())).takeLast(OFFSET_SAMPLES)
    }

    override fun now(): Instant = deviceClock.now() + serverOffset
}

/** The middle value, or the mean of the middle two for an even count; zero for no samples. */
private fun List<Duration>.median(): Duration {
    if (isEmpty()) return Duration.ZERO
    val sorted = sorted()
    val middle = size / 2
    return if (size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
}
