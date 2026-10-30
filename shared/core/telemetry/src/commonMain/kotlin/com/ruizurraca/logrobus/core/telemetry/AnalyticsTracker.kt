package com.ruizurraca.logrobus.core.telemetry

/**
 * Records analytics events (ADR-0009). Shared code and the platform ViewModels track through this contract only; a
 * vendor implementation lives in the platform app and is passed in at start-up, so no vendor SDK reaches shared code.
 *
 * Calls come from any thread, background ones included, and on the caller's thread: an implementation must be
 * thread-safe and must not block.
 */
interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}
