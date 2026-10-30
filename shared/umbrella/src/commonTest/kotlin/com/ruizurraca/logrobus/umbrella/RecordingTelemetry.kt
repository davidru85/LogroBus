package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.telemetry.AnalyticsEvent
import com.ruizurraca.logrobus.core.telemetry.AnalyticsTracker
import com.ruizurraca.logrobus.core.telemetry.Breadcrumb
import com.ruizurraca.logrobus.core.telemetry.ConsentDecision.GRANTED
import com.ruizurraca.logrobus.core.telemetry.CrashArea
import com.ruizurraca.logrobus.core.telemetry.CrashReporter
import com.ruizurraca.logrobus.core.telemetry.MemoryConsentStore
import com.ruizurraca.logrobus.core.telemetry.TelemetryConsent
import org.koin.core.Koin
import kotlin.test.assertEquals

/** A stand-in for a platform's vendor telemetry (ADR-0009): it records what reaches it. */
internal class RecordingTelemetry :
    AnalyticsTracker,
    CrashReporter {
    val received = mutableListOf<String>()

    override fun track(event: AnalyticsEvent) {
        received += "track $event"
    }

    override fun recordNonFatal(
        error: Throwable,
        area: CrashArea,
    ) {
        received += "non-fatal in $area"
    }

    override fun breadcrumb(crumb: Breadcrumb) {
        received += "breadcrumb $crumb"
    }
}

/**
 * ADR-0009: the [native] telemetry a platform passed to `initKoin` is the one the graph uses, behind the consent gates:
 * nothing reaches it until the user consents.
 */
internal fun assertTheGraphUsesTheNativeTelemetry(
    koin: Koin,
    native: RecordingTelemetry,
) {
    val event = AnalyticsEvent.LineView(lineId = 4)
    val crumb = Breadcrumb.ScreenView(Breadcrumb.Screen.LINE_DETAIL)
    koin.get<AnalyticsTracker>().track(event)
    koin.get<CrashReporter>().breadcrumb(crumb)
    assertEquals(emptyList(), native.received, "Telemetry reached the native implementation before consent")

    koin.get<MemoryConsentStore>().update(TelemetryConsent(analytics = GRANTED, crashReports = GRANTED))
    koin.get<AnalyticsTracker>().track(event)
    koin.get<CrashReporter>().breadcrumb(crumb)
    koin.get<CrashReporter>().recordNonFatal(IllegalStateException(), CrashArea.UI)

    assertEquals(listOf("track $event", "breadcrumb $crumb", "non-fatal in UI"), native.received)
}
