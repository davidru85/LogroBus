package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.network.ClientIdentity
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** ADR-0014 on iOS: the graph resolves every dependency the app has. */
class IosGraphTest {
    @AfterTest
    fun stop() = stopKoin()

    @Test
    fun lb010_theIosGraphResolvesEveryCoreDependency() {
        initGraphForTest(appVersion = "1.2.0", debug = true, telemetry = PlatformTelemetry())

        assertResolvesTheCore(KoinPlatform.getKoin())
        // The path a Swift ViewModel takes by default. Kotlin/Native has no reflection, so each use case is listed;
        // the Android host test checks every function of UseCases.
        SharedDependencies.observeLastSyncs()
        SharedDependencies.observeLinesWithFavourites()
        SharedDependencies.setFavouriteLine()
        SharedDependencies.reorderFavouriteLines()
        SharedDependencies.observePreferences()
        SharedDependencies.setTheme()
        SharedDependencies.setEtaFormat()
        SharedDependencies.clearCache()
        SharedDependencies.observeLocationAccess()
        SharedDependencies.readInitialOrigin()
    }

    @Test
    fun lb017_initKoinTakesTheAppsOwnTelemetry() {
        // The injection point for the Swift vendor implementations of LB-409 (ADR-0009). The graph is started by
        // `initGraphForTest`, the same modules as `initKoin` without the app-start refresh, which would race this
        // test's own teardown on the database file (lb008_database).
        val native = RecordingTelemetry()

        initGraphForTest(
            appVersion = "1.2.0",
            debug = false,
            telemetry = PlatformTelemetry(analytics = native, crashReporter = native),
        )

        assertTheGraphUsesTheNativeTelemetry(KoinPlatform.getKoin(), native)
    }

    @Test
    fun lb010_theAppShellSetsTheVersion() {
        initGraphForTest(appVersion = "1.2.0", debug = false, telemetry = PlatformTelemetry())

        // Spec §8.7: the version the operator sees in the User-Agent.
        assertEquals(
            "LogronoBus/1.2.0",
            KoinPlatform
                .getKoin()
                .get<ClientIdentity>()
                .userAgent
                .substringBefore(" "),
        )
    }
}
