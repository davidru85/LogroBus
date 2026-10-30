package com.ruizurraca.logrobus.core.telemetry

import com.ruizurraca.logrobus.core.common.Logger

object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) = Unit
}

object NoOpCrashReporter : CrashReporter {
    override fun recordNonFatal(
        error: Throwable,
        area: CrashArea,
    ) = Unit

    override fun breadcrumb(crumb: Breadcrumb) = Unit
}

class LoggingAnalyticsTracker(
    private val logger: Logger,
) : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) = Unit
}

class LoggingCrashReporter(
    private val logger: Logger,
) : CrashReporter {
    override fun recordNonFatal(
        error: Throwable,
        area: CrashArea,
    ) = Unit

    override fun breadcrumb(crumb: Breadcrumb) = Unit
}
