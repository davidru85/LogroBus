package com.ruizurraca.logrobus.android.feature.lines

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.ruizurraca.logrobus.android.core.designsystem.LogroBusTheme
import com.ruizurraca.logrobus.feature.lines.presentation.LinesUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * AGENTS.md §9 and spec §7.3: every state of the Lines list in light, dark and at 200 % font, in Spanish, on a window
 * tall enough for the whole list: at 200 %, every code shows whole and names wrap (LB-101). One capture per test, on
 * [SCREENSHOT_SDK].
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "es-h1600dp", sdk = [SCREENSHOT_SDK])
class LinesScreenshotTest(
    private val state: String,
    private val variant: String,
) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun lb101_linesState_screenshot() {
        val uiState = states.getValue(state)
        compose.setContent {
            val density = LocalDensity.current
            val fontScale = if (variant == FONT_SCALE_2) 2f else density.fontScale
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                LogroBusTheme(darkTheme = variant == DARK) {
                    Surface {
                        val search = rememberTextFieldState(queries[state].orEmpty())
                        LinesScreen(uiState, onEvent = {}, onOpenMore = {}, onOpenLine = {}, search = search)
                    }
                }
            }
        }

        compose.onRoot().captureRoboImage("src/test/screenshots/Lines_${state}_$variant.png")
    }

    companion object {
        private const val DARK = "dark"
        private const val FONT_SCALE_2 = "fontScale2"

        private val states: Map<String, LinesUiState> =
            mapOf(
                "loading" to LinesPreviewData.loading,
                "content" to LinesPreviewData.content,
                // LB-103: the carousel's cards, which "content" shows as its hint.
                "favourites" to LinesPreviewData.favourites,
                // LB-102: a search's results, without the carousel, and a search that found nothing.
                "search" to LinesPreviewData.search,
                "noMatches" to LinesPreviewData.noMatches,
                "empty" to LinesPreviewData.empty,
                "error" to LinesPreviewData.error,
            )

        // What the search field shows in each state: the query that made it.
        private val queries: Map<String, String> =
            mapOf("search" to LinesPreviewData.SEARCH, "noMatches" to LinesPreviewData.NO_MATCHES)

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun cases(): List<Array<String>> =
            states.keys.flatMap { state -> listOf("light", DARK, FONT_SCALE_2).map { arrayOf(state, it) } }
    }
}
