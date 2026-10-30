package com.ruizurraca.logrobus.core.telemetry

class ConsentGatedAnalyticsTracker(
    private val delegate: AnalyticsTracker,
    private val consentStore: ConsentStore,
) : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) = delegate.track(event)
}

class ConsentGatedCrashReporter(
    private val delegate: CrashReporter,
    private val consentStore: ConsentStore,
) : CrashReporter {
    override fun recordNonFatal(
        error: Throwable,
        area: CrashArea,
    ) = delegate.recordNonFatal(error, area)

    override fun breadcrumb(crumb: Breadcrumb) = delegate.breadcrumb(crumb)
}
