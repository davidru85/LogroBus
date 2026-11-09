package com.ruizurraca.logrobus.android.app

import com.ruizurraca.logrobus.android.feature.lines.linesUiModule
import com.ruizurraca.logrobus.android.feature.nearme.nearMeUiModule
import com.ruizurraca.logrobus.android.feature.planner.plannerUiModule
import com.ruizurraca.logrobus.android.feature.settings.settingsUiModule
import com.ruizurraca.logrobus.umbrella.previewDataModule
import com.ruizurraca.logrobus.umbrella.startPreviewGraph
import org.koin.core.context.stopKoin

/**
 * The Application of every Robolectric test in this module (robolectric.properties). Robolectric starts a new
 * Application, and so a new graph, for every test in one process, and calls onTerminate after each, so the graph
 * stops there: no test class has to remember it.
 *
 * [startGraph] is the one production method it replaces: a screen test needs a catalogue to look at, and since LB-008
 * part 2 the real graph downloads from a source the test guard refuses (LB-003) and leaves the cache empty. The
 * preview's scripted data module stands in, with the app-start refresh off. `AndroidGraphTest` covers the production
 * graph itself.
 */
class TestLogroBusApplication : LogroBusApplication() {
    override fun startGraph() {
        startPreviewGraph(
            this,
            data = previewDataModule(),
            uiModules = listOf(linesUiModule, nearMeUiModule, plannerUiModule, settingsUiModule, shellModule),
        )
    }

    override fun onTerminate() {
        stopKoin()
        super.onTerminate()
    }
}
