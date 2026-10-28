package com.ruizurraca.logrobus.android.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** design_guidelines.md §6.1 and §6.2, NFR-A11Y-02 and NFR-A11Y-06: how a line is shown. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LineBadgeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    @Config(qualifiers = "es")
    fun lb011_aLineBadgeSaysItsLine_neverItsColour() {
        compose.setContent { LogroBusTheme { LineBadge("3", 0xFFC100FB, 0xFF000000) } }

        compose.onNodeWithContentDescription("Línea 3").assertExists()
    }

    @Test
    @Config(qualifiers = "en")
    fun lb011_aLineBadgeSaysItsLine_inEnglish() {
        compose.setContent { LogroBusTheme { LineBadge("B1", 0xFFFD0200, 0xFF000000) } }

        compose.onNodeWithContentDescription("Line B1").assertExists()
    }

    @Test
    fun lb011_everyLineBadgeSizeIsAtLeast32By24() {
        compose.setContent {
            LogroBusTheme {
                Column {
                    LineBadgeSize.entries.forEach { size ->
                        LineBadge("1", 0xFF269905, 0xFF000000, Modifier.testTag(size.name), size)
                    }
                }
            }
        }

        LineBadgeSize.entries.forEach { size ->
            compose.onNodeWithTag(size.name).assertWidthIsAtLeast(32.dp).assertHeightIsAtLeast(24.dp)
        }
    }

    @Test
    @Config(sdk = [SCREENSHOT_SDK])
    fun lb011_theCurrentLines_screenshots() {
        captureRoboImage("src/test/screenshots/LineBadge_light.png") { CurrentLineBadges(darkTheme = false) }
        captureRoboImage("src/test/screenshots/LineBadge_dark.png") { CurrentLineBadges(darkTheme = true) }
        captureRoboImage("src/test/screenshots/LineBadge_fontScale2.png") {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
                CurrentLineBadges(darkTheme = false)
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun CurrentLineBadges(darkTheme: Boolean) {
        LogroBusTheme(darkTheme) {
            Surface {
                Column(Modifier.padding(8.dp)) {
                    LineBadgeSize.entries.forEach { size ->
                        FlowRow(Modifier.padding(4.dp)) {
                            currentLines.forEach { (code, color, onColor) ->
                                LineBadge(code, color, onColor, Modifier.padding(2.dp), size)
                            }
                        }
                    }
                }
            }
        }
    }
}
