package com.ruizurraca.logrobus.core.common

import kotlin.concurrent.Volatile
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/** The only source of "now" for business logic (AGENTS.md §5.5): never call the device clock directly. */
fun interface AppClock {
    fun now(): Instant
}

/** Spec §8.6: how many recent `Date` samples the server offset is averaged over. */
private const val OFFSET_SAMPLES = 5

/**
 * The device clock corrected by the server offset (spec §8.6, FR-ERR-06). The network layer passes every BFF
 * response's `Date` header to [onServerDate]. The offset is the mean of the last [OFFSET_SAMPLES] samples, so one
 * slow response does not move it much. Until the first sample arrives, the offset is zero.
 */
class ServerCorrectedClock(
    private val deviceClock: Clock,
) : AppClock {
    // Replaced, never mutated. Two responses that land at once can lose one sample, which the mean absorbs.
    @Volatile
    private var offsets: List<Duration> = emptyList()

    /** `serverDate - deviceNow`, averaged over the recent samples. */
    val serverOffset: Duration
        get() = offsets.let { samples -> if (samples.isEmpty()) Duration.ZERO else samples.sum() / samples.size }

    fun onServerDate(serverDate: Instant) {
        offsets = (offsets + (serverDate - deviceClock.now())).takeLast(OFFSET_SAMPLES)
    }

    override fun now(): Instant = deviceClock.now() + serverOffset

    private fun List<Duration>.sum(): Duration = fold(Duration.ZERO, Duration::plus)
}
