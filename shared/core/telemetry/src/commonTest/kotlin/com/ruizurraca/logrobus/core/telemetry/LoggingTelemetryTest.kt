package com.ruizurraca.logrobus.core.telemetry

import com.ruizurraca.logrobus.core.common.ConsoleLogger
import com.ruizurraca.logrobus.core.telemetry.AnalyticsEvent.PlanResult
import kotlin.test.Test
import kotlin.test.assertEquals

/** ADR-0009: debug builds print telemetry to the console, through the Logger and its privacy rules. */
class LoggingTelemetryTest {
    private val lines = mutableListOf<String>()
    private val logger = ConsoleLogger(output = { lines += it })

    @Test
    fun lb017_theLoggingTrackerPrintsEachEvent() {
        val tracker = LoggingAnalyticsTracker(logger)

        tracker.track(AnalyticsEvent.AppOpen)
        tracker.track(PlanResult(itineraryCount = 0, rejection = PlanResult.Rejection.NO_ROUTE))

        assertEquals(
            listOf(
                "D/Telemetry: track AppOpen",
                "D/Telemetry: track PlanResult(itineraryCount=0, rejection=NO_ROUTE)",
            ),
            lines,
        )
    }

    @Test
    fun lb017_theLoggingReporterPrintsBreadcrumbsAndErrorsWithoutTheirMessages() {
        val reporter = LoggingCrashReporter(logger)

        reporter.breadcrumb(Breadcrumb.ScreenView(Breadcrumb.Screen.PLANNER))
        // A message can quote what the user typed (NFR-PRIV-02): only the error's type is printed.
        reporter.recordNonFatal(IllegalStateException("from Calle Portales"), CrashArea.NETWORK)

        assertEquals(
            listOf(
                "D/Telemetry: breadcrumb ScreenView(screen=PLANNER)",
                "W/Telemetry: non-fatal in NETWORK [IllegalStateException]",
            ),
            lines,
        )
    }
}
