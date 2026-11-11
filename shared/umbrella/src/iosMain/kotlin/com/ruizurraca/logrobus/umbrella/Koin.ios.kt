package com.ruizurraca.logrobus.umbrella

import app.cash.sqldelight.db.SqlDriver
import com.ruizurraca.logrobus.core.common.AppDispatchers
import com.ruizurraca.logrobus.core.common.AppLifecycle
import com.ruizurraca.logrobus.core.common.NotificationAppLifecycle
import com.ruizurraca.logrobus.core.database.logroBusDriver
import com.ruizurraca.logrobus.core.location.LocationProvider
import com.ruizurraca.logrobus.core.location.coreLocationProvider
import com.ruizurraca.logrobus.core.network.clientIdentity
import com.ruizurraca.logrobus.core.preferences.PreferencesStore
import com.ruizurraca.logrobus.core.preferences.userDefaultsPreferencesStore
import com.ruizurraca.logrobus.feature.settings.data.appCaches
import com.ruizurraca.logrobus.feature.settings.domain.AppCaches
import org.koin.dsl.module
import org.koin.dsl.onClose
import platform.Foundation.NSUserDefaults

/**
 * Starts the dependency graph of the iOS app, once, from the app's initialiser on the main thread (ADR-0014). The app
 * passes its [appVersion] for the User-Agent (spec §8.7) and whether it is a [debug] build, from its own build
 * configuration rather than the framework's, which sets the log level (security_privacy.md §4), the telemetry and,
 * until LB-008 part 2, the sample lines. It returns nothing, so no Koin type reaches Swift.
 *
 * [telemetry] is the injection point for the app's own telemetry: Swift classes conforming to the exported protocols,
 * the vendor implementations of LB-409. It stays behind the consent gates; a part left `nil` falls back to the build's
 * no-op or logging telemetry (ADR-0009). Swift sees no default arguments, so the app always passes it.
 */
fun initKoin(
    appVersion: String,
    debug: Boolean,
    telemetry: PlatformTelemetry,
) {
    startGraph(
        sharedModules(iosModule(appVersion, debug), telemetryModule(debug, telemetry)),
        // The app-start refresh (architecture.md §4.4). Previews and the Swift tests use
        // `SharedDependencies.preview()`, which never starts this graph.
        startInitialSync = true,
    )
}

/**
 * The same graph without the app-start refresh, for `IosGraphTest`. The refresh writes the catalogue in a coroutine
 * that outlives the test and races the next test's schema creation on the single database file, which SQLiter reports
 * as `database is locked`. The refresh is not what the graph test checks, and `initKoin` still starts it for the app.
 */
internal fun initGraphForTest(
    appVersion: String,
    debug: Boolean,
    telemetry: PlatformTelemetry,
) {
    startGraph(
        sharedModules(iosModule(appVersion, debug), telemetryModule(debug, telemetry)),
        startInitialSync = false,
    )
}

/**
 * The graph a UI test needs, beside [initKoin] and `SharedDependencies.preview()` (ADR-0007): the same modules, with
 * [previewDataModule] in place of the transit data one and with the app-start refresh off. A UI test starts it through
 * its launch argument, so it sees the scripted lines rather than whatever the live source answers, which would make the
 * test depend on which buses are running (LB-008 part 2).
 */
fun initPreviewKoin(
    appVersion: String,
    debug: Boolean,
    telemetry: PlatformTelemetry,
) {
    startGraph(
        sharedModules(iosModule(appVersion, debug), telemetryModule(debug, telemetry), previewDataModule()),
        startInitialSync = false,
    )
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
    single<PreferencesStore> {
        userDefaultsPreferencesStore(get(), NSUserDefaults.standardUserDefaults, get<AppDispatchers>().io)
    }
    single<AppCaches> { appCaches(get<AppDispatchers>().io, get()) }
}
