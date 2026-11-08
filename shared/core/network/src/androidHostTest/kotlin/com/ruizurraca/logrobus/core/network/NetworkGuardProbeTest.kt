package com.ruizurraca.logrobus.core.network

import java.net.InetAddress
import java.net.UnknownHostException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * LB-003: the guard that `:shared:core:testing` registers reaches an ordinary module through the convention, so this
 * module's tests cannot call the live source (docs/testing_strategy.md §1 rule 2). A plain test: the guard works in
 * the JVM's resolver, before any sandbox is involved.
 */
class NetworkGuardProbeTest {
    @Test
    fun lb003_theSourceHostIsUnresolvable() {
        val failure = assertFailsWith<UnknownHostException> { InetAddress.getByName("transporteurbano.logrono.es") }

        assertTrue(failure.message.orEmpty().contains("NETWORK GUARD"), failure.message.orEmpty())
    }

    @Test
    fun lb003_localhostStillResolves() {
        assertTrue(InetAddress.getByName("localhost").isLoopbackAddress)
        assertEquals("127.0.0.1", InetAddress.getByName("127.0.0.1").hostAddress)
    }
}
