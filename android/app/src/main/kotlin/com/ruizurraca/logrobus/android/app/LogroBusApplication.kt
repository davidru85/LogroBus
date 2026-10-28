package com.ruizurraca.logrobus.android.app

import android.app.Application
import com.ruizurraca.logrobus.umbrella.initKoin

/** Starts the dependency graph once per process, on the main thread, before any screen (ADR-0014). */
class LogroBusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // The version for the User-Agent (spec §8.7), and the build type for the log level (security_privacy.md §4).
        initKoin(this, appVersion = BuildConfig.VERSION_NAME, debug = BuildConfig.DEBUG)
    }
}
