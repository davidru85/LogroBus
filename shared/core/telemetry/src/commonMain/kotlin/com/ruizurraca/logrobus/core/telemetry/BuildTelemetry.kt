package com.ruizurraca.logrobus.core.telemetry

import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.debug
import com.ruizurraca.logrobus.core.common.warn

/** Release builds until a vendor is chosen (ADR-0009, OQ-16): events go nowhere. */
object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) = Unit
}

/** Release builds until a vendor is chosen (ADR-0009, OQ-16): reports go nowhere. */
object NoOpCrashReporter : CrashReporter {
    override fun recordNonFatal(
        error: Throwable,
        area: CrashArea,
    ) = Unit

    override fun breadcrumb(crumb: Breadcrumb) = Unit
}

/**
 * Debug builds (ADR-0009): prints each event to the console. Behind the consent gate, nothing prints until the user
 * consents, which only LB-409 lets them do; until then, tests check the events.
 */
class LoggingAnalyticsTracker(
    private val logger: Logger,
) : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) = logger.debug(TAG, "track $event")
}

/** Debug builds (ADR-0009): prints breadcrumbs, and non-fatal errors by area and types, never their messages. */
class LoggingCrashReporter(
    private val logger: Logger,
) : CrashReporter {
    override fun recordNonFatal(
        error: Throwable,
        area: CrashArea,
    ) = logger.warn(TAG, "non-fatal in $area [${error.typeChain()}]")

    override fun breadcrumb(crumb: Breadcrumb) = logger.debug(TAG, "breadcrumb $crumb")
}

private const val TAG = "Telemetry"
