package com.ruizurraca.logrobus.android.feature.planner

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.feature.planner.domain.PlanPlace
import com.ruizurraca.logrobus.feature.planner.domain.ReadInitialOrigin
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Instant

/**
 * FR-PLAN-11 on Android: the origin follows the location permission whenever the root shows again, such as back from
 * the system's settings, where the user may have turned location on or off. FR-PLAN-08: the swap button reaches the
 * ViewModel.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlannerRouteTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    // What the platform's permission reads now.
    private var origin: PlanPlace? = null

    @After
    fun stopGraph() = stopKoin()

    @Test
    fun lb301_backFromTheBackground_theOriginFollowsThePermission() {
        startGraph()
        compose.setContent { PlannerRoute(onOpenMore = {}) }
        compose.onNodeWithContentDescription(origin(R.string.plan_origin_hint)).assertExists()

        // Granted in the system's settings while the app was in the background.
        origin = PlanPlace.MyLocation
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        compose.onNodeWithContentDescription(origin(R.string.plan_my_location)).assertExists()
    }

    @Test
    fun lb304_oneTapOnSwap_exchangesTheFields() {
        origin = PlanPlace.MyLocation
        startGraph()
        compose.setContent { PlannerRoute(onOpenMore = {}) }

        compose.onNodeWithContentDescription(context.getString(R.string.plan_swap)).performClick()

        compose.onNodeWithContentDescription(origin(R.string.plan_origin_hint)).assertExists()
        compose.onNodeWithContentDescription(destination(R.string.plan_my_location)).assertExists()
    }

    private fun startGraph() {
        startKoin {
            modules(
                plannerUiModule,
                module {
                    factory { ReadInitialOrigin { origin } }
                    single { AppClock { Instant.parse("2026-10-22T05:00:00Z") } }
                },
            )
        }
    }

    private fun origin(shown: Int) = context.getString(R.string.plan_origin_spoken, context.getString(shown))

    private fun destination(shown: Int) = context.getString(R.string.plan_destination_spoken, context.getString(shown))
}
