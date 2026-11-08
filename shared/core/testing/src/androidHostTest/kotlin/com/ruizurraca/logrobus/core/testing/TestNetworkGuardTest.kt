package com.ruizurraca.logrobus.core.testing

import java.net.InetAddress
import java.net.UnknownHostException
import java.net.spi.InetAddressResolverProvider
import java.util.ServiceLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * LB-003: the JDK installs [TestNetworkGuard] through the service file this module ships, so a test that resolves a
 * real host fails instead of reaching the live source (docs/testing_strategy.md §1 rule 2).
 *
 * A plain test, without Robolectric: the guard lives in the JVM's own resolver, and the sandbox would get in the way of
 * what is being checked.
 */
class TestNetworkGuardTest {
    @Test
    fun lb003_theServiceLoaderFindsTheGuard() {
        val providers = ServiceLoader.load(InetAddressResolverProvider::class.java).toList()

        assertEquals(1, providers.size, "exactly one provider: the guard")
        assertEquals(NETWORK_GUARD_NAME, providers.single().name())
        assertIs<TestNetworkGuard>(providers.single())
    }

    @Test
    fun lb003_aRealHostThrowsWithTheGuardMarker() {
        val failure = assertFailsWith<UnknownHostException> { InetAddress.getByName(LIVE_HOST) }

        // The host is named; no address and no coordinates are (NFR-PRIV-02).
        assertTrue(failure.message.orEmpty().startsWith(NETWORK_GUARD_MARKER), failure.message.orEmpty())
        assertTrue(failure.message.orEmpty().contains(LIVE_HOST), failure.message.orEmpty())
    }

    @Test
    fun lb003_loopbackStillResolves() {
        assertEquals("127.0.0.1", InetAddress.getByName("127.0.0.1").hostAddress)
        assertTrue(InetAddress.getByName("localhost").isLoopbackAddress)
    }

    private companion object {
        /** The source's host, which no test may resolve (AGENTS.md §9). */
        const val LIVE_HOST = "transporteurbano.logrono.es"
    }
}
