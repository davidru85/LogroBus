package com.ruizurraca.logrobus.core.telemetry

/**
 * Reports what went wrong without crashing, and what the user did before (ADR-0009). Like [AnalyticsTracker], it is a
 * contract whose vendor implementation the platform app passes in at start-up, and it is called from any thread: an
 * implementation must be thread-safe and must not block.
 */
interface CrashReporter {
    /**
     * Records [error], caught in [area]. Behind the consent gate, an implementation receives a [RedactedThrowable],
     * whose messages are gone: a message can quote a URL or a payload (NFR-PRIV-02).
     */
    fun recordNonFatal(
        error: Throwable,
        area: CrashArea,
    )

    /** Leaves a trail mark for the next report: enums only, never free text (NFR-PRIV-02). */
    fun breadcrumb(crumb: Breadcrumb)
}

/** Where a non-fatal error was caught. */
enum class CrashArea {
    NETWORK,
    DATA,
    DATABASE,
    SYNC,
    POLLING,
    LOCATION,
    UI,
}

/** A step before a crash. Like analytics events, breadcrumbs carry ids, counts, flags and enums only (ADR-0009). */
sealed interface Breadcrumb {
    /** A screen was shown. */
    data class ScreenView(
        val screen: Screen,
    ) : Breadcrumb

    /** The MVP screens (spec §7, ADR-0002). */
    enum class Screen {
        LINES,
        LINE_DETAIL,
        NEAR_ME,
        STOP_ARRIVALS,
        PLANNER,
        PLAN_RESULTS,
        MORE,
        SETTINGS,
        ABOUT_DATA,
    }
}
