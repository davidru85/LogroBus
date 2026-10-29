package com.ruizurraca.logrobus.android.feature.settings

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.feature.settings.domain.DatasetSync
import com.ruizurraca.logrobus.feature.settings.domain.ObserveLastSyncs
import com.ruizurraca.logrobus.transit.domain.StaticDataset
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.compose.koinViewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.annotation.GraphicsMode

/** ADR-0014 on Android: a screen gets its ViewModel with koinViewModel(), and keeps it across configuration changes. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AboutDataRouteTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun startGraph() {
        val neverSynced = StaticDataset.entries.map { DatasetSync(it, null) }
        startKoin {
            modules(
                module { factory { ObserveLastSyncs { flowOf(AppResult.Success(neverSynced)) } } },
                settingsUiModule,
            )
        }
    }

    @After
    fun stopGraph() = stopKoin()

    @Test
    fun lb010_theRouteShowsTheStateOfItsViewModelFromTheGraph() {
        compose.setContent { AboutDataRoute(onBack = {}) }

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

    @Test
    fun lb010_eachScreenGetsItsOwnViewModel() {
        // viewModelOf, not single: another owner, such as another back stack entry (LB-012), gets a new one.
        var screen: AboutDataViewModel? = null
        var other: AboutDataViewModel? = null
        val otherOwner =
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        compose.setContent {
            screen = koinViewModel()
            CompositionLocalProvider(LocalViewModelStoreOwner provides otherOwner) { other = koinViewModel() }
        }

        assertNotNull(screen)
        assertNotSame(screen, other)
        otherOwner.viewModelStore.clear()
    }
}
