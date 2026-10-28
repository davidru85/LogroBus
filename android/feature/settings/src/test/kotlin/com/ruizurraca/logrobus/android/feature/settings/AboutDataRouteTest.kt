package com.ruizurraca.logrobus.android.feature.settings

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.logrobus.feature.settings.domain.DatasetSync
import com.ruizurraca.logrobus.feature.settings.domain.ObserveLastSyncs
import com.ruizurraca.logrobus.transit.domain.StaticDataset
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.compose.koinViewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/** ADR-0014 on Android: a screen gets its ViewModel with koinViewModel(), and keeps it across configuration changes. */
@RunWith(AndroidJUnit4::class)
class AboutDataRouteTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun startGraph() {
        val neverSynced = StaticDataset.entries.map { DatasetSync(it, null) }
        startKoin {
            modules(module { factory { ObserveLastSyncs { flowOf(neverSynced) } } }, settingsUiModule)
        }
    }

    @After
    fun stopGraph() = stopKoin()

    @Test
    fun lb010_theRouteShowsTheStateOfItsViewModelFromTheGraph() {
        compose.setContent { AboutDataRoute() }

        compose.onNodeWithText(compose.activity.getString(R.string.about_dataset_lines)).assertExists()
        compose.onNodeWithText(compose.activity.getString(R.string.about_dataset_stops)).assertExists()
        compose
            .onAllNodesWithText(compose.activity.getString(R.string.about_last_sync_never))
            .assertCountEquals(StaticDataset.entries.size)
    }

    @Test
    fun lb010_theViewModelSurvivesAConfigurationChange() {
        var before: AboutDataViewModel? = null
        var after: AboutDataViewModel? = null
        compose.setContent { before = koinViewModel() }

        compose.activityRule.scenario.recreate()
        compose.runOnUiThread { compose.activity.setContent { after = koinViewModel() } }
        compose.waitForIdle()

        assertNotNull(before)
        assertSame(before, after)
    }
}
