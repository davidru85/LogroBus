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
 * The app's telemetry (ADR-0009), each part behind its consent gate. The platform's own [analytics] and
 * [crashReporter], the vendor implementations of LB-409, are used when given; otherwise the build's: logging in debug
 * builds and no-op in release builds. One consent store serves the whole app.
 */
internal fun telemetryModule(
    debug: Boolean,
    analytics: AnalyticsTracker? = null,
    crashReporter: CrashReporter? = null,
): Module =
    module {
        single { MemoryConsentStore() } bind ConsentStore::class
        single<AnalyticsTracker> {
            val tracker = analytics ?: if (debug) LoggingAnalyticsTracker(get()) else NoOpAnalyticsTracker
            ConsentGatedAnalyticsTracker(tracker, get())
        }
        single<CrashReporter> {
            val reporter = crashReporter ?: if (debug) LoggingCrashReporter(get()) else NoOpCrashReporter
            ConsentGatedCrashReporter(reporter, get())
        }
    }
