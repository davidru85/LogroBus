package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.common.ConsoleLogger
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.telemetry.AnalyticsEvent
import com.ruizurraca.logrobus.core.telemetry.AnalyticsTracker
import com.ruizurraca.logrobus.core.telemetry.Breadcrumb
import com.ruizurraca.logrobus.core.telemetry.ConsentDecision.GRANTED
import com.ruizurraca.logrobus.core.telemetry.ConsentStore
import com.ruizurraca.logrobus.core.telemetry.CrashArea
import com.ruizurraca.logrobus.core.telemetry.CrashReporter
import com.ruizurraca.logrobus.core.telemetry.MemoryConsentStore
import com.ruizurraca.logrobus.core.telemetry.TelemetryConsent
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * ADR-0009: release builds bind the no-op telemetry and debug builds the logging one, both behind the consent gates.
 * The graph's logger prints every level here, so a logging implementation would show in a release build too.
 */
class TelemetryGraphTest {
    private val printed = mutableListOf<String>()

    @AfterTest
    fun stop() = stopKoin()

    private fun startGraph(debug: Boolean) {
        val platform = module { single<Logger> { ConsoleLogger(output = { line -> printed += line }) } }
        startGraph(sharedModules(platform, telemetryModule(debug)))
    }

    private fun consentToEverything() =
        KoinPlatform
            .getKoin()
            .get<MemoryConsentStore>()
            .update(TelemetryConsent(analytics = GRANTED, crashReports = GRANTED))

    private fun sendTelemetry() {
        val koin = KoinPlatform.getKoin()
        koin.get<AnalyticsTracker>().track(AnalyticsEvent.AppOpen)
        koin.get<CrashReporter>().breadcrumb(Breadcrumb.ScreenView(Breadcrumb.Screen.LINES))
        koin.get<CrashReporter>().recordNonFatal(IllegalStateException(), CrashArea.SYNC)
    }

    private fun printedTelemetry() = printed.filter { "/Telemetry:" in it }

    @Test
    fun lb017_aReleaseBuildBindsTheNoOpImplementations() {
        startGraph(debug = false)
        consentToEverything()

        sendTelemetry()

        assertEquals(emptyList(), printedTelemetry())
    }

    @Test
    fun lb017_aDebugBuildBindsTheLoggingImplementations() {
        startGraph(debug = true)
        consentToEverything()

        sendTelemetry()

        assertEquals(
            listOf(
                "D/Telemetry: track AppOpen",
                "D/Telemetry: breadcrumb ScreenView(screen=LINES)",
                "W/Telemetry: non-fatal in SYNC [IllegalStateException]",
            ),
            printedTelemetry(),
        )
    }

    @Test
    fun lb017_theGraphsTelemetryWaitsForConsent() {
        startGraph(debug = true)

        sendTelemetry()

        assertEquals(emptyList(), printedTelemetry())
    }

    @Test
    fun lb017_theAppHasOneConsentStore() {
        // The gates read the store that the consent dialog of LB-409 will write.
        startGraph(debug = true)
        val koin = KoinPlatform.getKoin()

        assertSame(koin.get<MemoryConsentStore>(), koin.get<ConsentStore>())
    }
}
