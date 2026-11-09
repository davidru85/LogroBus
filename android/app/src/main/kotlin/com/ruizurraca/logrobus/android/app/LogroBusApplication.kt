package com.ruizurraca.logrobus.android.app

import android.app.Application
import com.ruizurraca.logrobus.android.feature.lines.linesUiModule
import com.ruizurraca.logrobus.android.feature.nearme.nearMeUiModule
import com.ruizurraca.logrobus.android.feature.planner.plannerUiModule
import com.ruizurraca.logrobus.android.feature.settings.settingsUiModule
import com.ruizurraca.logrobus.umbrella.initKoin

/**
 * Starts the dependency graph once per process, on the main thread, before any screen (ADR-0014).
 */
open class LogroBusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startGraph()
    }

    /**
     * The graph this process runs (ADR-0014). A test subclass overrides exactly this to start the preview's scripted
     * graph instead, so one process starts one graph: two would fail at Koin's `allowOverride(false)`.
     *
     * The version is for the User-Agent (spec §8.7) and the build type sets the log level and the telemetry
     * (security_privacy.md §4, ADR-0009). LB-409 passes the vendor's analytics tracker and crash reporter here too, as
     * `telemetry = PlatformTelemetry(…)`.
     */
    protected open fun startGraph() {
        initKoin(
            this,
            appVersion = BuildConfig.VERSION_NAME,
            debug = BuildConfig.DEBUG,
            uiModules = listOf(linesUiModule, nearMeUiModule, plannerUiModule, settingsUiModule, shellModule),
        )
    }
}
