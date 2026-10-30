package com.ruizurraca.logrobus.core.telemetry

import com.ruizurraca.logrobus.core.telemetry.ConsentDecision.DENIED
import com.ruizurraca.logrobus.core.telemetry.ConsentDecision.GRANTED
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** ADR-0009, NFR-PRIV-03: crash reporting is opt-in too, for non-fatal errors and breadcrumbs alike. */
class ConsentGatedCrashReporterTest {
    private val consent = MemoryConsentStore()
    private val reported = mutableListOf<String>()
    private val errors = mutableListOf<Throwable>()
    private val gate = ConsentGatedCrashReporter(delegate = RecordingReporter(reported, errors), consentStore = consent)
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

    @Test
    fun lb017_theReporterBehindTheGateGetsNoErrorMessage() {
        // NFR-PRIV-02: a message can quote a request URL with what the user typed, or a payload with coordinates.
        consent.update(TelemetryConsent(crashReports = GRANTED))
        val error = IllegalStateException("GET /candidates?q=Calle Portales", IllegalArgumentException("lat=42.4650"))

        gate.recordNonFatal(error, CrashArea.NETWORK)

        val received = errors.single()
        val texts = generateSequence(received) { it.cause }.map { it.message.orEmpty() } + received.stackTraceToString()
        assertTrue(texts.none { "Calle" in it || "42.4650" in it }, texts.joinToString(" | "))
    }

    @Test
    fun lb017_theReporterBehindTheGateGetsTheErrorTypesAndStack() {
        consent.update(TelemetryConsent(crashReports = GRANTED))
        val error = IllegalStateException("outer", IllegalArgumentException("inner"))

        gate.recordNonFatal(error, CrashArea.DATA)

        val received = assertIs<RedactedThrowable>(errors.single())
        val cause = assertIs<RedactedThrowable>(received.cause)
        assertTrue(received.type.endsWith("IllegalStateException"), received.type)
        assertTrue(cause.type.endsWith("IllegalArgumentException"), cause.type)
        assertEquals(received.type, received.message)
        assertTrue(received.frames.isNotEmpty())
    }

    private class RecordingReporter(
        private val reported: MutableList<String>,
        private val errors: MutableList<Throwable>,
    ) : CrashReporter {
        override fun recordNonFatal(
            error: Throwable,
            area: CrashArea,
        ) {
            reported += "non-fatal $area"
            errors += error
        }

        override fun breadcrumb(crumb: Breadcrumb) {
            reported += "breadcrumb $crumb"
        }
    }
}
