package com.ruizurraca.logrobus.android.feature.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.ruizurraca.logrobus.android.core.designsystem.LogroBusTheme
import com.ruizurraca.logrobus.feature.settings.presentation.SettingsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * AGENTS.md §9: Settings' states (LB-403), its first load and its content, in light, dark and at 200 % font, in
 * Spanish. The screen is tall enough to show every row.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "es-h1400dp", sdk = [SCREENSHOT_SDK])
class SettingsScreenshotTest(
    private val state: String,
    private val variant: String,
) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun lb403_settings_screenshot() {
        val shown = if (state == "loading") SettingsUiState.Loading else SettingsPreviewData.content
        compose.setContent {
            val density = LocalDensity.current
            val fontScale = if (variant == "fontScale2") 2f else density.fontScale
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                LogroBusTheme(darkTheme = variant == "dark") { SettingsScreen(shown, onEvent = {}, onBack = {}) }
            }
        }

        compose.onRoot().captureRoboImage("src/test/screenshots/Settings_${state}_$variant.png")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun cases(): List<Array<String>> =
            listOf("loading", "content").flatMap { state ->
                listOf("light", "dark", "fontScale2").map { arrayOf(state, it) }
            }
    }
}
