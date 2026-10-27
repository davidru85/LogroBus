package com.ruizurraca.logrobus.core.network

import android.os.Build
import com.ruizurraca.logrobus.core.common.NoOpLogger
import com.ruizurraca.logrobus.core.common.ServerCorrectedClock
import io.ktor.client.engine.okhttp.OkHttpConfig
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/** ADR-0012 and spec §8.7 on Android. */
@RunWith(RobolectricTestRunner::class)
class AndroidPlatformTest {
    @Test
    fun lb005_androidIdentifiesItselfWithItsVersion() {
        assertTrue(Build.VERSION.RELEASE.isNotBlank())
        assertEquals(ClientIdentity("1.2.0", "Android", Build.VERSION.RELEASE), clientIdentity("1.2.0"))
    }

    @Test
    fun lb005_androidCallsGoThroughOkHttp() {
        val clock = ServerCorrectedClock(
            object : Clock {
                override fun now() = Instant.parse("2026-10-27T08:00:00Z")
            },
        )

        val client = SourceClient(clientIdentity("1.2.0"), clock, { "https://transporteurbano.logrono.es" }, NoOpLogger)

        assertIs<OkHttpConfig>(client.http.engine.config)
    }
}
