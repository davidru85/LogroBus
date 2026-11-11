package com.ruizurraca.logrobus.android.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
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
        compose.show(variant) { Screen() }
        // The shell opens on Lines (FR-NAV-02), and that list arrives from the catalogue on the I/O threads: a capture
        // taken before it does shows the loading placeholder, which is a different screen and has raced CI once.
        if (screen == "shell") {
            compose.waitUntil(AWAIT_MILLIS) {
                compose.onAllNodes(hasScrollToIndexAction()).fetchSemanticsNodes().isNotEmpty()
            }
        }

        compose.onRoot().captureRoboImage("src/test/screenshots/${screen}_$variant.png")
    }

    /** LB-406 (NFR-A11Y): the tabs and More, labelled, large enough and whole at 200 %. */
    @Test
    fun lb406_shellScreen_isAccessible() {
        // Labels, touch targets and text do not change with the colours: dark repeats light.
        assumeTrue(variant != "dark")
        compose.show(variant) { Screen() }

        compose.assertAccessible()
    }

    @Composable
    private fun Screen() {
        when (screen) {
            "shell" -> AppShell()
            else -> MoreScreen(onOpenSettings = {}, onOpenAboutData = {}, onBack = {})
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
        compose.show(variant) { AppShell() }

        compose.onRoot().captureRoboImage("src/test/screenshots/shellRail_$variant.png")
    }

    /** LB-406 (NFR-A11Y): the navigation rail, labelled, large enough and whole at 200 %. */
    @Test
    fun lb406_theShellOnAMediumWindow_isAccessible() {
        // Labels, touch targets and text do not change with the colours: dark repeats light.
        assumeTrue(variant != "dark")
        compose.show(variant) { AppShell() }

        compose.assertAccessible()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<String>> = VARIANTS.map { arrayOf(it) }
    }
}

private val VARIANTS = listOf("light", "dark", "fontScale2")

private fun ComposeContentTestRule.show(
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
}

/** Far longer than a catalogue read takes, even on a busy runner. */
private const val AWAIT_MILLIS = 5_000L
