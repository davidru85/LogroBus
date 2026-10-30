package com.ruizurraca.logrobus.android.feature.settings

import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.ruizurraca.logrobus.android.core.designsystem.LogroBusTheme
import com.ruizurraca.logrobus.feature.settings.presentation.AboutDataUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * AGENTS.md §9 and spec §7.3: every state of About data in light, dark and at 200 % font, in Spanish. One capture per
 * test, on [SCREENSHOT_SDK].
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "es", sdk = [SCREENSHOT_SDK])
class AboutDataScreenshotTest(
    private val state: String,
    private val variant: String,
) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun lb011_aboutDataState_screenshot() {
        val uiState = states.getValue(state)
        compose.setContent {
            val density = LocalDensity.current
            val fontScale = if (variant == FONT_SCALE_2) 2f else density.fontScale
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                LogroBusTheme(
                    darkTheme = variant == DARK,
                ) { Surface { AboutDataScreen(uiState, onEvent = {}, onBack = {}) } }
            }
        }

        compose.onRoot().captureRoboImage("src/test/screenshots/AboutData_${state}_$variant.png")
    }

    companion object {
        private const val DARK = "dark"
        private const val FONT_SCALE_2 = "fontScale2"

        private val states: Map<String, AboutDataUiState> =
            mapOf(
                "loading" to AboutDataPreviewData.loading,
                "synced" to AboutDataPreviewData.synced,
                "neverSynced" to AboutDataPreviewData.neverSynced,
                "error" to AboutDataPreviewData.error,
            )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun cases(): List<Array<String>> =
            states.keys.flatMap { state -> listOf("light", DARK, FONT_SCALE_2).map { arrayOf(state, it) } }
    }
}
