package com.ruizurraca.logrobus.android.feature.nearme

import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.ruizurraca.logrobus.android.core.designsystem.LogroBusTheme
import com.ruizurraca.logrobus.android.core.testing.assertAccessible
import com.ruizurraca.logrobus.feature.nearme.presentation.NearMeUiState
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * AGENTS.md §9: every state of Near Me in light, dark and at 200 % font, in Spanish. One capture per test, on
 * [SCREENSHOT_SDK].
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "es", sdk = [SCREENSHOT_SDK])
class NearMeScreenshotTest(
    private val state: String,
    private val variant: String,
) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun lb201_nearMeState_screenshot() {
        show()

        compose.onRoot().captureRoboImage("src/test/screenshots/NearMe_${state}_$variant.png")
    }

    /** LB-406 (NFR-A11Y): labels, touch targets, and at 200 % every row whole. */
    @Test
    fun lb406_nearMeState_isAccessible() {
        // Labels, touch targets and text do not change with the colours: dark repeats light.
        assumeTrue(variant != DARK)
        show()

        compose.assertAccessible()
    }

    private fun show() {
        val uiState = states.getValue(state)
        compose.setContent {
            val density = LocalDensity.current
            val fontScale = if (variant == FONT_SCALE_2) 2f else density.fontScale
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                LogroBusTheme(darkTheme = variant == DARK) {
                    Surface { NearMeScreen(uiState, onTurnOnLocation = {}, onOpenMore = {}) }
                }
            }
        }
    }

    companion object {
        private const val DARK = "dark"
        private const val FONT_SCALE_2 = "fontScale2"

        private val states: Map<String, NearMeUiState> =
            mapOf(
                "turnOnLocation" to NearMePreviewData.turnOnLocation,
                "locationOn" to NearMePreviewData.locationOn,
            )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun cases(): List<Array<String>> =
            states.keys.flatMap { state -> listOf("light", DARK, FONT_SCALE_2).map { arrayOf(state, it) } }
    }
}
