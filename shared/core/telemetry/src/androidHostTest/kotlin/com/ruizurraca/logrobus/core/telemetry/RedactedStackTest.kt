package com.ruizurraca.logrobus.core.telemetry

import com.ruizurraca.logrobus.core.telemetry.ConsentDecision.GRANTED
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertIs

/** ADR-0009 on Android: a vendor that reads the Throwable itself, as Crashlytics does, sees where the error came from. */
class RedactedStackTest {
    @Test
    fun lb017_onAndroidTheRedactedErrorKeepsTheOriginalStack() {
        val errors = mutableListOf<Throwable>()
        val delegate =
            object : CrashReporter {
                override fun recordNonFatal(
                    error: Throwable,
                    area: CrashArea,
                ) {
                    errors += error
                }

                override fun breadcrumb(crumb: Breadcrumb) = Unit
            }
        val gate =
            ConsentGatedCrashReporter(delegate, MemoryConsentStore(TelemetryConsent(crashReports = GRANTED)))
        val error = IllegalStateException("q=Calle Portales")

        gate.recordNonFatal(error, CrashArea.NETWORK)

        assertContentEquals(error.stackTrace, assertIs<RedactedThrowable>(errors.single()).stackTrace)
    }
}
