package com.ruizurraca.logrobus.core.network

import android.os.Build
import com.ruizurraca.logrobus.core.common.NoOpLogger
import io.ktor.client.engine.okhttp.OkHttpConfig
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

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
        val client = SourceClient(clientIdentity("1.2.0"), stoppedClock(), { BASE_URL }, NoOpLogger)

        client.http.use { assertIs<OkHttpConfig>(it.engine.config) }
    }
}
