package com.ruizurraca.logrobus.umbrella

import android.content.Context
import android.content.pm.ApplicationInfo
import app.cash.sqldelight.db.SqlDriver
import com.ruizurraca.logrobus.core.common.AppLifecycle
import com.ruizurraca.logrobus.core.common.ProcessAppLifecycle
import com.ruizurraca.logrobus.core.database.logroBusDriver
import com.ruizurraca.logrobus.core.location.LocationProvider
import com.ruizurraca.logrobus.core.location.fusedLocationProvider
import com.ruizurraca.logrobus.core.network.clientIdentity
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * Starts the dependency graph of the Android app, once per process, from the Application (ADR-0014). It returns
 * nothing, so no Koin type reaches the app shell.
 */
fun initKoin(context: Context) {
    startKoin { modules(coreModules(androidModule(context.applicationContext))) }
}

private fun androidModule(context: Context) =
    module {
        single { appLogger(debug = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) }
        // Created at start, on the main thread that starts Koin.
        single<AppLifecycle>(createdAtStart = true) { ProcessAppLifecycle() }
        single<LocationProvider>(createdAtStart = true) { fusedLocationProvider(context, get()) }
        single {
            clientIdentity(
                appVersion =
                    context.packageManager
                        .getPackageInfo(context.packageName, 0)
                        .versionName
                        .orEmpty(),
            )
        }
        single<SqlDriver> { logroBusDriver(context) }
    }
