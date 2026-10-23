package com.ruizurraca.logrobus.android.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** LB-001: the app launches without crashing, on the start tab of FR-NAV-02, in both shipped languages. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MainActivityTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    @Config(qualifiers = "en")
    fun lb001_appLaunchesOnTheLinesTab() {
        compose.onNodeWithText("Lines").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "es")
    fun lb001_appLaunchesOnTheLinesTab_inSpanish() {
        compose.onNodeWithText("Líneas").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "night")
    fun lb001_systemBarIconsStayDarkOnTheLightPlaceholder_inDarkMode() {
        // The placeholder window is always light, so the bar icons must stay dark even in dark mode.
        val window = compose.activity.window
        val bars = WindowCompat.getInsetsController(window, window.decorView)
        assertTrue("status bar icons are dark", bars.isAppearanceLightStatusBars)
        assertTrue("navigation bar icons are dark", bars.isAppearanceLightNavigationBars)
    }
}
