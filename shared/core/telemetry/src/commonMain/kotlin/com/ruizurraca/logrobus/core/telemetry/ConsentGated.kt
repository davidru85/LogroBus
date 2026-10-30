package com.ruizurraca.logrobus.core.telemetry

/**
 * Forwards events to [delegate] only while the user consents to analytics (NFR-PRIV-03, ADR-0009). Before the user
 * decides, and after a refusal, events are dropped, never kept for later.
 */
class ConsentGatedAnalyticsTracker(
    private val delegate: AnalyticsTracker,
    private val consentStore: ConsentStore,
) : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) {
        if (consentStore.consent.value.analytics == ConsentDecision.GRANTED) delegate.track(event)
    }
}

/**
 * Forwards non-fatal errors and breadcrumbs to [delegate] only while the user consents to crash reports. An error is
 * forwarded as a [RedactedThrowable], with its types and stack and none of its messages (NFR-PRIV-02).
 */
class ConsentGatedCrashReporter(
    private val delegate: CrashReporter,
    private val consentStore: ConsentStore,
) : CrashReporter {
    override fun recordNonFatal(
        error: Throwable,
        area: CrashArea,
    ) {
        if (granted()) delegate.recordNonFatal(error.redacted(), area)
    }

    override fun breadcrumb(crumb: Breadcrumb) {
        if (granted()) delegate.breadcrumb(crumb)
    }

    private fun granted(): Boolean = consentStore.consent.value.crashReports == ConsentDecision.GRANTED
}
