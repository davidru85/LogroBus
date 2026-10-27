package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.common.AppDispatchers
import com.ruizurraca.logrobus.core.common.AppLifecycle
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.ServerCorrectedClock
import com.ruizurraca.logrobus.core.config.RemoteConfig
import com.ruizurraca.logrobus.core.database.LogroBusDatabase
import com.ruizurraca.logrobus.core.location.LocationProvider
import com.ruizurraca.logrobus.core.network.SourceClient
import com.ruizurraca.logrobus.core.polling.PollingEngine
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertSame

/** ADR-0014 on Android: the graph resolves every dependency the app has, so a missing binding fails here. */
@RunWith(RobolectricTestRunner::class)
class AndroidGraphTest {
    @AfterTest
    fun stop() = stopKoin()

    @Test
    fun lb010_theAndroidGraphResolvesEveryCoreDependency() {
        val koin = initKoin(RuntimeEnvironment.getApplication()).koin

        listOf(
            Logger::class,
            AppDispatchers::class,
            AppLifecycle::class,
            LocationProvider::class,
            RemoteConfig::class,
            SourceClient::class,
            PollingEngine::class,
            LogroBusDatabase::class,
        ).forEach { koin.get<Any>(it) }
        // One clock for the whole app: the source client corrects it, and the polling engine and screens read it.
        assertSame(koin.get<ServerCorrectedClock>(), koin.get<AppClock>())
    }
}
