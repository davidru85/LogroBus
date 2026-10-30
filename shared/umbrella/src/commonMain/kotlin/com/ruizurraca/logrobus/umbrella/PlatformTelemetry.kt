package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.telemetry.AnalyticsTracker
import com.ruizurraca.logrobus.core.telemetry.ConsentGatedAnalyticsTracker
import com.ruizurraca.logrobus.core.telemetry.ConsentGatedCrashReporter
import com.ruizurraca.logrobus.core.telemetry.ConsentStore
import com.ruizurraca.logrobus.core.telemetry.CrashReporter
import com.ruizurraca.logrobus.core.telemetry.LoggingAnalyticsTracker
import com.ruizurraca.logrobus.core.telemetry.LoggingCrashReporter
import com.ruizurraca.logrobus.core.telemetry.MemoryConsentStore
import com.ruizurraca.logrobus.core.telemetry.NoOpAnalyticsTracker
import com.ruizurraca.logrobus.core.telemetry.NoOpCrashReporter
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * The app's own telemetry, which it passes to `initKoin`: the injection point for the vendor implementations of LB-409
 * (ADR-0009). On iOS, Swift classes conforming to the exported protocols. A part left `null` falls back to the build's
 * telemetry. Both are called from any thread, so they must be thread-safe.
 *
 * The consent gates stand in front of every call made through the contracts. They cannot stop what a vendor SDK
 * collects by itself, such as uncaught crashes or automatic screen views: LB-409 must ship the SDK with that
 * collection off and tie it to the user's consent.
 */
class PlatformTelemetry(
    val analytics: AnalyticsTracker? = null,
    val crashReporter: CrashReporter? = null,
)

/**
 * The app's telemetry (ADR-0009), each part behind its consent gate: the [platform]'s own where given, otherwise the
 * build's, logging in debug builds and no-op in release builds. One consent store serves the whole app.
 */
internal fun telemetryModule(
    debug: Boolean,
    platform: PlatformTelemetry = PlatformTelemetry(),
): Module =
    module {
        single { MemoryConsentStore() } bind ConsentStore::class
        single<AnalyticsTracker> {
            val tracker = platform.analytics ?: if (debug) LoggingAnalyticsTracker(get()) else NoOpAnalyticsTracker
            ConsentGatedAnalyticsTracker(tracker, get())
        }
        single<CrashReporter> {
            val reporter = platform.crashReporter ?: if (debug) LoggingCrashReporter(get()) else NoOpCrashReporter
            ConsentGatedCrashReporter(reporter, get())
        }
    }
