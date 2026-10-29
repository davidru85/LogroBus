package com.ruizurraca.logrobus.android.feature.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.ruizurraca.logrobus.android.core.designsystem.LogroBusTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** AGENTS.md §9: Settings, a placeholder until LB-403, in light, dark and at 200 % font, in Spanish. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "es", sdk = [SCREENSHOT_SDK])
class SettingsScreenshotTest(
    private val variant: String,
) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun lb012_settings_screenshot() {
        compose.setContent {
            val density = LocalDensity.current
            val fontScale = if (variant == "fontScale2") 2f else density.fontScale
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                LogroBusTheme(darkTheme = variant == "dark") { SettingsScreen(onBack = {}) }
            }
        }

        compose.onRoot().captureRoboImage("src/test/screenshots/Settings_$variant.png")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<String>> = listOf("light", "dark", "fontScale2").map { arrayOf(it) }
    }
}
