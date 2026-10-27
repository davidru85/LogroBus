package com.ruizurraca.logrobus.umbrella

import app.cash.sqldelight.db.SqlDriver
import com.ruizurraca.logrobus.core.common.AppLifecycle
import com.ruizurraca.logrobus.core.common.NotificationAppLifecycle
import com.ruizurraca.logrobus.core.database.logroBusDriver
import com.ruizurraca.logrobus.core.location.LocationProvider
import com.ruizurraca.logrobus.core.location.coreLocationProvider
import com.ruizurraca.logrobus.core.network.clientIdentity
import org.koin.core.context.startKoin
import org.koin.dsl.module
import platform.Foundation.NSBundle
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

/**
 * Starts the dependency graph of the iOS app, once, from the app's initialiser on the main thread (ADR-0014). It
 * returns nothing, so no Koin type reaches Swift.
 */
fun initKoin() {
    startKoin { modules(coreModules(iosModule)) }
}

@OptIn(ExperimentalNativeApi::class) // Platform.isDebugBinary
private val iosModule =
    module {
        single { appLogger(debug = Platform.isDebugBinary) }
        // Created at start, on the main thread that starts Koin: Core Location needs a run loop (LB-004).
        single<AppLifecycle>(createdAtStart = true) { NotificationAppLifecycle() }
        single<LocationProvider>(createdAtStart = true) { coreLocationProvider(get()) }
        single {
            val version = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
            clientIdentity(appVersion = version.orEmpty())
        }
        single<SqlDriver> { logroBusDriver() }
    }
