package com.ruizurraca.logrobus.core.telemetry

import com.ruizurraca.logrobus.core.telemetry.ConsentDecision.DENIED
import com.ruizurraca.logrobus.core.telemetry.ConsentDecision.GRANTED
import kotlin.test.Test
import kotlin.test.assertEquals

/** ADR-0009, NFR-PRIV-03: crash reporting is opt-in too, for non-fatal errors and breadcrumbs alike. */
class ConsentGatedCrashReporterTest {
    private val consent = MemoryConsentStore()
    private val reported = mutableListOf<String>()
    private val gate = ConsentGatedCrashReporter(delegate = RecordingReporter(reported), consentStore = consent)
    private val error = IllegalStateException()
    private val crumb = Breadcrumb.ScreenView(Breadcrumb.Screen.NEAR_ME)

    @Test
    fun lb017_beforeTheUserDecidesNothingIsReported() {
        gate.recordNonFatal(error, CrashArea.NETWORK)
        gate.breadcrumb(crumb)

        assertEquals(emptyList(), reported)
    }

    @Test
    fun lb017_onceTheUserConsentsToCrashReportsErrorsAndBreadcrumbsAreForwarded() {
        consent.update(TelemetryConsent(crashReports = GRANTED))

        gate.breadcrumb(crumb)
        gate.recordNonFatal(error, CrashArea.NETWORK)

        assertEquals(listOf("breadcrumb $crumb", "non-fatal NETWORK"), reported)
    }

    @Test
    fun lb017_afterTheUserWithdrawsConsentNothingIsReported() {
        consent.update(TelemetryConsent(crashReports = GRANTED))
        consent.update(TelemetryConsent(crashReports = DENIED))

        gate.recordNonFatal(error, CrashArea.DATABASE)
        gate.breadcrumb(crumb)

        assertEquals(emptyList(), reported)
    }

    @Test
    fun lb017_consentToAnalyticsAloneReportsNothing() {
        // NFR-PRIV-03: the toggles are granular.
        consent.update(TelemetryConsent(analytics = GRANTED))

        gate.recordNonFatal(error, CrashArea.SYNC)
        gate.breadcrumb(crumb)

        assertEquals(emptyList(), reported)
    }

    private class RecordingReporter(
        private val reported: MutableList<String>,
    ) : CrashReporter {
        override fun recordNonFatal(
            error: Throwable,
            area: CrashArea,
        ) {
            reported += "non-fatal $area"
        }

        override fun breadcrumb(crumb: Breadcrumb) {
            reported += "breadcrumb $crumb"
        }
    }
}
