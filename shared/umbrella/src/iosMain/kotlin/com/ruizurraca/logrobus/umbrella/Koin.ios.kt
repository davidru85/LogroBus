package com.ruizurraca.logrobus.umbrella

import app.cash.sqldelight.db.SqlDriver
import com.ruizurraca.logrobus.core.common.AppLifecycle
import com.ruizurraca.logrobus.core.common.NotificationAppLifecycle
import com.ruizurraca.logrobus.core.database.logroBusDriver
import com.ruizurraca.logrobus.core.location.LocationProvider
import com.ruizurraca.logrobus.core.location.coreLocationProvider
import com.ruizurraca.logrobus.core.network.clientIdentity
import com.ruizurraca.logrobus.core.telemetry.AnalyticsTracker
import com.ruizurraca.logrobus.core.telemetry.CrashReporter
import org.koin.dsl.module
import org.koin.dsl.onClose

/**
 * Starts the dependency graph of the iOS app, once, from the app's initialiser on the main thread (ADR-0014). The app
 * passes its [appVersion] for the User-Agent (spec §8.7) and whether it is a [debug] build, from its own build
 * configuration rather than the framework's, which sets the log level (security_privacy.md §4) and the telemetry. It
 * returns nothing, so no Koin type reaches Swift.
 *
 * [analytics] and [crashReporter] are the injection point for the app's own telemetry: Swift classes conforming to the
 * exported protocols, the vendor implementations of LB-409. They stay behind the consent gates; with `nil`, the
 * build's no-op or logging telemetry is used (ADR-0009). Swift sees no default arguments, so the app passes both.
 */
fun initKoin(
    appVersion: String,
    debug: Boolean,
    analytics: AnalyticsTracker?,
    crashReporter: CrashReporter?,
) {
    startGraph(sharedModules(iosModule(appVersion, debug), telemetryModule(debug, analytics, crashReporter)))
}

private fun iosModule(
    appVersion: String,
    debug: Boolean,
) = module {
    single { appLogger(debug) }
    // Created at start, on the main thread that starts Koin: Core Location needs a run loop (LB-004).
    single<AppLifecycle>(createdAtStart = true) { NotificationAppLifecycle() }
    single<LocationProvider>(createdAtStart = true) { coreLocationProvider(get()) }
    single { clientIdentity(appVersion) }
    single<SqlDriver> { logroBusDriver() } onClose { it?.close() }
}
