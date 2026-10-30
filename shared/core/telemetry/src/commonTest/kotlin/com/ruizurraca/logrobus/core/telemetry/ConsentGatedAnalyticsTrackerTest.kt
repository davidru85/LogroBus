package com.ruizurraca.logrobus.core.telemetry

import com.ruizurraca.logrobus.core.telemetry.ConsentDecision.DENIED
import com.ruizurraca.logrobus.core.telemetry.ConsentDecision.GRANTED
import kotlin.test.Test
import kotlin.test.assertEquals

/** ADR-0009, NFR-PRIV-03: analytics is opt-in, so nothing reaches the tracker behind the gate without consent. */
class ConsentGatedAnalyticsTrackerTest {
    private val consent = MemoryConsentStore()
    private val tracked = mutableListOf<AnalyticsEvent>()
    private val gate = ConsentGatedAnalyticsTracker(delegate = RecordingTracker(tracked), consentStore = consent)

    @Test
    fun lb017_beforeTheUserDecidesNoEventIsForwarded() {
        gate.track(AnalyticsEvent.AppOpen)

        assertEquals(emptyList(), tracked)
    }

    @Test
    fun lb017_onceTheUserConsentsToAnalyticsEventsAreForwarded() {
        consent.update(TelemetryConsent(analytics = GRANTED))

        gate.track(AnalyticsEvent.StopView(stopId = 152))

        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.StopView(stopId = 152)), tracked)
    }

    @Test
    fun lb017_eventsDroppedBeforeConsentAreNotSentLater() {
        gate.track(AnalyticsEvent.AppOpen)

        consent.update(TelemetryConsent(analytics = GRANTED))
        gate.track(AnalyticsEvent.LineView(lineId = 4))

        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.LineView(lineId = 4)), tracked)
    }

    @Test
    fun lb017_afterTheUserWithdrawsConsentNoEventIsForwarded() {
        consent.update(TelemetryConsent(analytics = GRANTED))
        consent.update(TelemetryConsent(analytics = DENIED))

        gate.track(AnalyticsEvent.AppOpen)

        assertEquals(emptyList(), tracked)
    }

    @Test
    fun lb017_consentToCrashReportsAloneForwardsNoEvent() {
        // NFR-PRIV-03: the toggles are granular.
        consent.update(TelemetryConsent(crashReports = GRANTED))

        gate.track(AnalyticsEvent.AppOpen)

        assertEquals(emptyList(), tracked)
    }

    private class RecordingTracker(
        private val tracked: MutableList<AnalyticsEvent>,
    ) : AnalyticsTracker {
        override fun track(event: AnalyticsEvent) {
            tracked += event
        }
    }
}
