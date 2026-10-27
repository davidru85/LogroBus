package com.ruizurraca.logrobus.android.app

import android.app.Application
import com.ruizurraca.logrobus.umbrella.initKoin

/** Starts the dependency graph once per process, on the main thread, before any screen (ADR-0014). */
class LogroBusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(this)
    }
}
