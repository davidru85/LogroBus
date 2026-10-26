package com.ruizurraca.logrobus.core.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

/** Spec §8.6: polling intervals come from remote config, with a floor of 10 s whatever the backend says. */
class PollIntervalTest {
    /** A backend that overrides some keys, as LB-015's will. */
    private class OverriddenRemoteConfig(
        private val overrides: Map<String, Any>,
    ) : RemoteConfig {
        @Suppress("UNCHECKED_CAST")
        override fun <T : Any> get(key: RemoteConfigKey<T>): T = (overrides[key.name] ?: key.default) as T
    }

    @Test
    fun lb004_pollingIntervalsUnder10000MsAreClampedTo10000Ms() {
        val config = OverriddenRemoteConfig(mapOf("poll_vehicles_ms" to 2_000, "poll_arrivals_ms" to 9_999))

        assertEquals(10.seconds, config.pollInterval(RemoteConfigKey.PollVehiclesMs))
        assertEquals(10.seconds, config.pollInterval(RemoteConfigKey.PollArrivalsMs))
    }

    @Test
    fun lb004_pollingIntervalsFrom10000MsUpAreKept() {
        val config = OverriddenRemoteConfig(mapOf("poll_vehicles_ms" to 10_000, "poll_arrivals_ms" to 15_000))

        assertEquals(10.seconds, config.pollInterval(RemoteConfigKey.PollVehiclesMs))
        assertEquals(15.seconds, config.pollInterval(RemoteConfigKey.PollArrivalsMs))
    }

    @Test
    fun lb004_defaultPollingIntervalsAre10SecondsForVehiclesAnd20ForArrivals() {
        val config = CompiledDefaultsRemoteConfig()

        assertEquals(10.seconds, config.pollInterval(RemoteConfigKey.PollVehiclesMs))
        assertEquals(20.seconds, config.pollInterval(RemoteConfigKey.PollArrivalsMs))
    }
}
