package com.ruizurraca.logrobus.android.feature.lines

import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.ruizurraca.logrobus.android.core.designsystem.LogroBusTheme
import com.ruizurraca.logrobus.android.core.testing.assertAccessible
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * LB-110 (FR-FAV-02): the favourite lines' edit sheet in light, dark and at 200 % font, in Spanish: each row with its
 * remove button, its badge and name or "no longer served", and its drag handle. The sheet's content, without the
 * modal window around it, which Robolectric captures apart, on a window tall enough for every row at 200 %. One
 * capture per test, on [SCREENSHOT_SDK].
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "es-h1000dp", sdk = [SCREENSHOT_SDK])
class FavouriteLinesEditorScreenshotTest(
    private val variant: String,
) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun lb110_theEditSheet_screenshot() {
        show()

        compose.onRoot().captureRoboImage("src/test/screenshots/FavouriteLinesEditor_$variant.png")
    }

    /** LB-406 (NFR-A11Y): labels, touch targets, and at 200 % every code and name whole. */
    @Test
    fun lb406_theEditSheet_isAccessible() {
        // Labels, touch targets and text do not change with the colours: dark repeats light.
        assumeTrue(variant != DARK)
        show()

        compose.assertAccessible()
    }

    private fun show() {
        compose.setContent {
            val density = LocalDensity.current
            val fontScale = if (variant == FONT_SCALE_2) 2f else density.fontScale
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                LogroBusTheme(darkTheme = variant == DARK) {
                    Surface {
                        FavouriteLinesEditor(LinesPreviewData.editable, onReorder = {}, onRemove = {}, onDone = {})
                    }
                }
            }
        }
    }

    companion object {
        private const val DARK = "dark"
        private const val FONT_SCALE_2 = "fontScale2"

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<String>> = listOf("light", DARK, FONT_SCALE_2).map { arrayOf(it) }
    }
}
