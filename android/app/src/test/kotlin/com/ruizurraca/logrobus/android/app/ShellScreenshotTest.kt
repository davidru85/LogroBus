package com.ruizurraca.logrobus.android.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.ComposeContentTestRule
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

/**
 * AGENTS.md §9: the shell on a phone, with its navigation bar, and More, in light, dark and at 200 % font, in Spanish.
 * One capture per test, on [SCREENSHOT_SDK].
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "es", sdk = [SCREENSHOT_SDK])
class ShellScreenshotTest(
    private val screen: String,
    private val variant: String,
) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun lb012_shellScreen_screenshot() {
        compose.capture("${screen}_$variant", variant) {
            when (screen) {
                "shell" -> AppShell()
                else -> MoreScreen(onOpenSettings = {}, onOpenAboutData = {}, onBack = {})
            }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun cases(): List<Array<String>> =
            listOf("shell", "more").flatMap { screen ->
                VARIANTS.map { arrayOf(screen, it) }
            }
    }
}

/** The shell on a medium window, with its navigation rail (FR-NAV-05), in light, dark and at 200 % font. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "es-w720dp-h540dp", sdk = [SCREENSHOT_SDK])
class RailScreenshotTest(
    private val variant: String,
) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun lb012_theShellOnAMediumWindow_screenshot() {
        compose.capture("shellRail_$variant", variant) { AppShell() }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<String>> = VARIANTS.map { arrayOf(it) }
    }
}

private val VARIANTS = listOf("light", "dark", "fontScale2")

private fun ComposeContentTestRule.capture(
    name: String,
    variant: String,
    content: @Composable () -> Unit,
) {
    setContent {
        val density = LocalDensity.current
        val fontScale = if (variant == "fontScale2") 2f else density.fontScale
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            LogroBusTheme(darkTheme = variant == "dark", content = content)
        }
    }
    onRoot().captureRoboImage("src/test/screenshots/$name.png")
}
