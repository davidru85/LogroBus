package com.ruizurraca.logrobus.android.app

import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.logrobus.android.core.designsystem.Tokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * LB-001: the app launches without crashing, on the start tab of FR-NAV-02, in both shipped languages. LB-011: it
 * draws on LogroBusTheme, in light and dark.
 */
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
    @Config(qualifiers = "notnight")
    fun lb011_systemBarIconsAreDarkOnTheLightTheme() {
        val window = compose.activity.window
        val bars = WindowCompat.getInsetsController(window, window.decorView)
        assertTrue("status bar icons are dark", bars.isAppearanceLightStatusBars)
        assertTrue("navigation bar icons are dark", bars.isAppearanceLightNavigationBars)
    }

    @Test
    @Config(qualifiers = "night")
    fun lb011_theAppFollowsDarkMode_withLightBarIcons() {
        // LogroBusTheme has a dark scheme (LB-011), so the window is no longer always light.
        val window = compose.activity.window
        val bars = WindowCompat.getInsetsController(window, window.decorView)
        assertFalse("status bar icons are light", bars.isAppearanceLightStatusBars)
        assertFalse("navigation bar icons are light", bars.isAppearanceLightNavigationBars)
    }

    @Test
    @Config(qualifiers = "night")
    fun lb011_theStartScreenDrawsOnTheThemesSurface() {
        compose.onRoot().captureToImage().toPixelMap().let { pixels ->
            assertEquals(Tokens.darkColors.surface, pixels[pixels.width / 2, pixels.height - 1])
        }
    }
}
