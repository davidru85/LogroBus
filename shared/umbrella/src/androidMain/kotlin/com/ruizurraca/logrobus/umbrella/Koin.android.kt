package com.ruizurraca.logrobus.umbrella

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import com.ruizurraca.logrobus.core.common.AppLifecycle
import com.ruizurraca.logrobus.core.common.ProcessAppLifecycle
import com.ruizurraca.logrobus.core.database.logroBusDriver
import com.ruizurraca.logrobus.core.location.LocationProvider
import com.ruizurraca.logrobus.core.location.fusedLocationProvider
import com.ruizurraca.logrobus.core.network.clientIdentity
import org.koin.dsl.module
import org.koin.dsl.onClose

/**
 * Starts the dependency graph of the Android app, once per process, from the Application (ADR-0014). The app passes
 * its [appVersion] for the User-Agent (spec §8.7) and whether it is a [debug] build, which sets the log level
 * (security_privacy.md §4). It returns nothing, so no Koin type reaches the app shell.
 */
fun initKoin(
    context: Context,
    appVersion: String,
    debug: Boolean,
) {
    startGraph(coreModules(androidModule(context.applicationContext, appVersion, debug)))
}

private fun androidModule(
    context: Context,
    appVersion: String,
    debug: Boolean,
) = module {
    single { appLogger(debug) }
    // Created at start, on the main thread that starts Koin.
    single<AppLifecycle>(createdAtStart = true) { ProcessAppLifecycle() }
    single<LocationProvider>(createdAtStart = true) { fusedLocationProvider(context, get()) }
    single { clientIdentity(appVersion) }
    single<SqlDriver> { logroBusDriver(context) } onClose { it?.close() }
}
