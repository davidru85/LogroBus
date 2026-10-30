package com.ruizurraca.logrobus.core.telemetry

import com.ruizurraca.logrobus.core.telemetry.ConsentDecision.GRANTED
import kotlin.test.Test

/**
 * ADR-0009: telemetry must never break the app. A vendor that throws, say because it was not initialised, would
 * otherwise turn an error a repository has handled into a crash.
 */
class FailingTelemetryTest {
    private val consent = MemoryConsentStore(TelemetryConsent(analytics = GRANTED, crashReports = GRANTED))

    @Test
    fun lb017_aFailingTrackerNeverBreaksTheCaller() {
        val gate = ConsentGatedAnalyticsTracker(FailingTelemetry, consent)

        gate.track(AnalyticsEvent.AppOpen)
    }

    @Test
    fun lb017_aFailingReporterNeverBreaksTheCaller() {
        val gate = ConsentGatedCrashReporter(FailingTelemetry, consent)

        gate.breadcrumb(Breadcrumb.ScreenView(Breadcrumb.Screen.LINES))
        gate.recordNonFatal(IllegalStateException(), CrashArea.DATABASE)
    }

    private object FailingTelemetry : AnalyticsTracker, CrashReporter {
        override fun track(event: AnalyticsEvent) = error("not initialised")

        override fun recordNonFatal(
            error: Throwable,
            area: CrashArea,
        ) = error("not initialised")

        override fun breadcrumb(crumb: Breadcrumb) = error("not initialised")
    }
}
