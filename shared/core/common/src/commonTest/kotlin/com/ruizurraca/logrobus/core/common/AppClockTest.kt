package com.ruizurraca.logrobus.core.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Spec §8.6 and FR-ERR-06: ETAs use the server's time, from the `Date` header of the BFF's responses. */
class AppClockTest {
    private class DeviceClock(
        var instant: Instant,
    ) : Clock {
        override fun now(): Instant = instant
    }

    private val serverNow = Instant.parse("2026-10-25T10:00:00Z")

    @Test
    fun lb004_appClockCorrectsADeviceClock90SecondsBehindTheServer() {
        val device = DeviceClock(serverNow - 90.seconds)
        val clock = ServerCorrectedClock(device)

        clock.onServerDate(serverNow)
        assertEquals(serverNow, clock.now())

        device.instant += 30.seconds
        assertEquals(serverNow + 30.seconds, clock.now())
    }

    @Test
    fun lb004_appClockUsesTheDeviceClockUntilTheFirstServerDate() {
        val device = DeviceClock(serverNow)
        assertEquals(serverNow, ServerCorrectedClock(device).now())
    }

    @Test
    fun lb004_serverOffsetIsSmoothedOverTheLastFiveSamples() {
        val device = DeviceClock(serverNow)
        val clock = ServerCorrectedClock(device)

        // Offsets of 10, 20, 30, 40, 50 and 60 s: the first leaves the window, so the mean of 20 to 60 s remains.
        listOf(10, 20, 30, 40, 50, 60).forEach { clock.onServerDate(serverNow + it.seconds) }

        assertEquals(40.seconds, clock.serverOffset)
        assertEquals(serverNow + 40.seconds, clock.now())
    }
}
