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
import com.ruizurraca.logrobus.core.telemetry.AnalyticsTracker
import com.ruizurraca.logrobus.core.telemetry.ConsentStore
import com.ruizurraca.logrobus.core.telemetry.CrashReporter
import org.koin.core.Koin
import kotlin.test.assertSame

/**
 * ADR-0014: [koin] resolves every dependency of the shared core, the telemetry contracts included (ADR-0009), so a
 * missing binding fails a test, not the app.
 */
internal fun assertResolvesTheCore(koin: Koin) {
    listOf(
        Logger::class,
        AppDispatchers::class,
        AppLifecycle::class,
        LocationProvider::class,
        RemoteConfig::class,
        SourceClient::class,
        PollingEngine::class,
        LogroBusDatabase::class,
        AnalyticsTracker::class,
        CrashReporter::class,
        ConsentStore::class,
    ).forEach { koin.get<Any>(it) }
    // One clock for the whole app: the source client corrects it, and the polling engine and screens read it.
    assertSame(koin.get<ServerCorrectedClock>(), koin.get<AppClock>())
}
