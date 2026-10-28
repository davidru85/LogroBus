package com.ruizurraca.logrobus.android.app

import org.koin.core.context.stopKoin

/**
 * The Application of every Robolectric test in this module (robolectric.properties). Robolectric starts a new
 * Application, and so a new graph, for every test in one process, and calls onTerminate after each, so the graph
 * stops there: no test class has to remember it.
 */
class TestLogroBusApplication : LogroBusApplication() {
    override fun onTerminate() {
        stopKoin()
        super.onTerminate()
    }
}
