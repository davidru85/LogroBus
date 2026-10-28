package com.ruizurraca.logrobus.android.app

import android.app.Application
import com.ruizurraca.logrobus.android.feature.settings.settingsUiModule
import com.ruizurraca.logrobus.umbrella.initKoin

/**
 * Starts the dependency graph once per process, on the main thread, before any screen (ADR-0014). Open for the tests'
 * TestLogroBusApplication, which stops the graph between tests.
 */
open class LogroBusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // The version for the User-Agent (spec §8.7), the build type for the log level (security_privacy.md §4), and
        // the ViewModels of each feature module.
        initKoin(
            this,
            appVersion = BuildConfig.VERSION_NAME,
            debug = BuildConfig.DEBUG,
            uiModules = listOf(settingsUiModule),
        )
    }
}
