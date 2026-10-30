package com.ruizurraca.logrobus.umbrella

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import com.ruizurraca.logrobus.core.common.AppLifecycle
import com.ruizurraca.logrobus.core.common.ProcessAppLifecycle
import com.ruizurraca.logrobus.core.database.logroBusDriver
import com.ruizurraca.logrobus.core.location.LocationProvider
import com.ruizurraca.logrobus.core.location.fusedLocationProvider
import com.ruizurraca.logrobus.core.network.clientIdentity
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose

/**
 * Starts the dependency graph of the Android app, once per process, from the Application (ADR-0014). The app passes
 * its [appVersion] for the User-Agent (spec §8.7), whether it is a [debug] build, which sets the log level
 * (security_privacy.md §4), and the [uiModules] of its feature modules, which hold their ViewModels.
 */
fun initKoin(
    context: Context,
    appVersion: String,
    debug: Boolean,
    uiModules: List<Module> = emptyList(),
) {
    startGraph(
        sharedModules(androidModule(context.applicationContext, appVersion, debug), telemetryModule(debug)) + uiModules,
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
}
