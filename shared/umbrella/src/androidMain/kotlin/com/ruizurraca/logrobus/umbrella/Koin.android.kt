package com.ruizurraca.logrobus.umbrella

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import com.ruizurraca.logrobus.core.common.AppDispatchers
import com.ruizurraca.logrobus.core.common.AppLifecycle
import com.ruizurraca.logrobus.core.common.ProcessAppLifecycle
import com.ruizurraca.logrobus.core.database.logroBusDriver
import com.ruizurraca.logrobus.core.location.LocationProvider
import com.ruizurraca.logrobus.core.location.fusedLocationProvider
import com.ruizurraca.logrobus.core.network.clientIdentity
import com.ruizurraca.logrobus.core.preferences.PreferencesStore
import com.ruizurraca.logrobus.core.preferences.dataStorePreferencesStore
import com.ruizurraca.logrobus.feature.settings.data.CacheDirectoryCaches
import com.ruizurraca.logrobus.feature.settings.domain.AppCaches
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose

/**
 * Starts the dependency graph of the Android app, once per process, from the Application (ADR-0014). The app passes
 * its [appVersion] for the User-Agent (spec §8.7), whether it is a [debug] build, which sets the log level
 * (security_privacy.md §4), the telemetry and, until LB-008 part 2, the sample lines, and the [uiModules] of its
 * feature modules, which hold their ViewModels.
 *
 * [telemetry] is the injection point for the app's own telemetry, the vendor implementations of LB-409. It stays behind
 * the consent gates; without it, the build's no-op or logging telemetry is used (ADR-0009).
 */
fun initKoin(
    context: Context,
    appVersion: String,
    debug: Boolean,
    uiModules: List<Module> = emptyList(),
    telemetry: PlatformTelemetry = PlatformTelemetry(),
) {
    startGraph(
        sharedModules(
            androidModule(context.applicationContext, appVersion, debug),
            telemetryModule(debug, telemetry),
            debug,
        ) + uiModules,
    )
}

private fun androidModule(
    context: Context,
    appVersion: String,
    debug: Boolean,
) = module {
    single { appLogger(debug) }
    // Created at start, on the main thread that starts Koin, as ProcessLifecycleOwner needs (LB-004).
    single<AppLifecycle>(createdAtStart = true) { ProcessAppLifecycle() }
    // Lazy: it takes its updates on the main looper whichever thread creates it, so it can wait for Near Me.
    single<LocationProvider> { fusedLocationProvider(context, get()) }
    single { clientIdentity(appVersion) }
    single<SqlDriver> { logroBusDriver(context) } onClose { it?.close() }
    // DataStore allows one active instance per file in a process (ADR-0018): its scope ends with the graph's, so a
    // later graph can read the settings again.
    single<PreferencesStore> {
        val graph = get<CoroutineScope>(APPLICATION_SCOPE).coroutineContext[Job]
        dataStorePreferencesStore(context, get(), CoroutineScope(get<AppDispatchers>().io + SupervisorJob(graph)))
    }
    single<AppCaches> { CacheDirectoryCaches(context.cacheDir, get<AppDispatchers>().io, get()) }
}
