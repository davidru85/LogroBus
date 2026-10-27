package com.ruizurraca.logrobus.core.network

import com.ruizurraca.logrobus.core.common.NoOpLogger
import com.ruizurraca.logrobus.core.common.ServerCorrectedClock
import io.ktor.client.engine.darwin.DarwinClientEngineConfig
import platform.Foundation.NSURLRequestReloadIgnoringLocalCacheData
import platform.Foundation.NSURLSessionConfiguration
import platform.UIKit.UIDevice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

/** ADR-0012 and spec §8.7 on iOS. */
class IosPlatformTest {
    @Test
    fun lb005_iosIdentifiesItselfWithItsVersion() {
        assertEquals(ClientIdentity("1.2.0", "iOS", UIDevice.currentDevice.systemVersion), clientIdentity("1.2.0"))
    }

    @Test
    fun lb005_iosCallsGoThroughNsUrlSessionWithoutCacheOrCookies() {
        // NSURLSession caches responses and stores cookies by default: a cached response would carry stale arrivals
        // and a stale Date, and spec §8.7 sends no cookies. OkHttp has neither unless asked.
        val clock = ServerCorrectedClock(
            object : Clock {
                override fun now() = Instant.parse("2026-10-27T08:00:00Z")
            },
        )
        val client = SourceClient(clientIdentity("1.2.0"), clock, { "https://transporteurbano.logrono.es" }, NoOpLogger)

        val engine = assertIs<DarwinClientEngineConfig>(client.http.engine.config)
        val session = NSURLSessionConfiguration.defaultSessionConfiguration().apply(engine.sessionConfig)

        assertNull(session.URLCache)
        assertEquals(NSURLRequestReloadIgnoringLocalCacheData, session.requestCachePolicy)
        assertNull(session.HTTPCookieStorage)
        assertFalse(session.HTTPShouldSetCookies)
    }
}
