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
 * The telemetry of the build (ADR-0009): logging in debug builds and no-op in release builds, each behind its consent
 * gate. One consent store serves the whole app.
 */
internal fun telemetryModule(debug: Boolean): Module =
    module {
        single { MemoryConsentStore() } bind ConsentStore::class
        single<AnalyticsTracker> {
            ConsentGatedAnalyticsTracker(if (debug) LoggingAnalyticsTracker(get()) else NoOpAnalyticsTracker, get())
        }
        single<CrashReporter> {
            ConsentGatedCrashReporter(if (debug) LoggingCrashReporter(get()) else NoOpCrashReporter, get())
        }
    }
