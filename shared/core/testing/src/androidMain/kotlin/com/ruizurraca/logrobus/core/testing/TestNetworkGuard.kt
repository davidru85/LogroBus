package com.ruizurraca.logrobus.core.testing

import java.net.InetAddress
import java.net.UnknownHostException
import java.net.spi.InetAddressResolver
import java.net.spi.InetAddressResolverProvider
import java.util.stream.Stream

/**
 * LB-003: no unit or integration test calls the live source (docs/testing_strategy.md §1, rule 2). This provider is
 * registered in `META-INF/services/java.net.spi.InetAddressResolverProvider`, so the JDK installs it as the resolver of
 * every JVM test that has `:shared:core:testing` on its test classpath: every host but loopback fails, and the message
 * starts with [NETWORK_GUARD_MARKER] and names the host — never an address, never coordinates (NFR-PRIV-02).
 *
 * `internal` is enough although the JDK instantiates it reflectively: Kotlin's `internal` is public in the bytecode,
 * and the class keeps the public no-argument constructor that [java.util.ServiceLoader] requires.
 *
 * Kotlin/Native has no JVM resolver, so the iOS simulator tests are not covered by this guard: they keep to
 * `MockEngine` and the preview fakes (§4.2–4.3). A literal IPv4 or IPv6 address is not resolved at all — the JDK
 * hands it back before asking any resolver — so only names are guarded, which is why the message names the host.
 */
internal class TestNetworkGuard : InetAddressResolverProvider() {
    override fun get(configuration: Configuration): InetAddressResolver =
        object : InetAddressResolver {
            override fun lookupByName(
                host: String,
                lookupPolicy: InetAddressResolver.LookupPolicy,
            ): Stream<InetAddress> =
                if (host.isLoopbackAddress()) {
                    configuration.builtinResolver().lookupByName(host, lookupPolicy)
                } else {
                    throw UnknownHostException("$NETWORK_GUARD_MARKER (docs/testing_strategy.md §1 rule 2): $host")
                }

            // Reverse lookups keep working: the address never leaves the machine.
            override fun lookupByAddress(addr: ByteArray): String =
                configuration.builtinResolver().lookupByAddress(addr)
        }

    override fun name(): String = NETWORK_GUARD_NAME
}

/** The provider's name, as the installed resolver reports it. */
internal const val NETWORK_GUARD_NAME: String = "LogroBus TestNetworkGuard"

/** Tests match the message on this prefix, never on the rest. */
internal const val NETWORK_GUARD_MARKER: String = "NETWORK GUARD: real network access is forbidden in tests"

/** Loopback only: `localhost`, `127.0.0.0/8` and `::1`. */
@Suppress("MagicNumber") // The four octets of an IPv4 address, and the largest value one takes.
private fun String.isLoopbackAddress(): Boolean {
    val text = removeSurrounding("[", "]")
    if (text.equals("localhost", ignoreCase = true)) return true
    if (text.contains(':')) return text == "::1" || text == "0:0:0:0:0:0:0:1"
    val octets = text.split('.')
    return octets.size == 4 &&
        octets[0] == "127" &&
        octets.drop(1).all { octet -> octet.toIntOrNull()?.let { it in 0..255 } ?: false }
}
