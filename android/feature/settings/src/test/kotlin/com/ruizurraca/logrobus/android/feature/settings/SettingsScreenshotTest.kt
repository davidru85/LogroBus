package com.ruizurraca.logrobus.android.feature.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.ruizurraca.logrobus.android.core.designsystem.LogroBusTheme
import com.ruizurraca.logrobus.android.core.testing.assertAccessible
import com.ruizurraca.logrobus.feature.settings.presentation.AppLanguage
import com.ruizurraca.logrobus.feature.settings.presentation.SettingsUiState
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * AGENTS.md §9: Settings' states (LB-403), its first load, its content and its content when the sync status cannot be
 * read, and the language dialog, in light, dark and at 200 % font, in Spanish. The screen is tall enough to show every
 * row.
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
        showSettings()

        compose.onRoot().captureRoboImage("src/test/screenshots/Settings_${state}_$variant.png")
    }

    /** LB-406 (NFR-A11Y): labels, touch targets, and at 200 % every choice whole. */
    @Test
    fun lb406_settings_isAccessible() {
        // Labels, touch targets and text do not change with the colours: dark repeats light.
        assumeTrue(variant != "dark")
        showSettings()

        compose.assertAccessible()
    }

    @Test
    fun lb403_languageDialog_screenshot() {
        if (state != "content") return
        showLanguageDialog()

        compose.onNode(isDialog()).captureRoboImage("src/test/screenshots/Settings_languageDialog_$variant.png")
    }

    /** LB-406 (NFR-A11Y): the dialog's choices, labelled, large enough and whole at 200 %. */
    @Test
    fun lb406_languageDialog_isAccessible() {
        // The dialog does not depend on the screen's state: it is checked once per variant, the others are skipped.
        assumeTrue(state == "content" && variant != "dark")
        showLanguageDialog()

        compose.assertAccessible()
    }

    private fun showSettings() {
        val shown =
            when (state) {
                "loading" -> SettingsUiState.Loading
                "unknown" -> SettingsPreviewData.unknown
                else -> SettingsPreviewData.content
            }
        compose.setContent {
            val density = LocalDensity.current
            val fontScale = if (variant == "fontScale2") 2f else density.fontScale
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                LogroBusTheme(darkTheme = variant == "dark") { SettingsScreen(shown, onEvent = {}, onBack = {}) }
            }
        }
    }

    private fun showLanguageDialog() {
        // A dialog is a window of its own, whose density comes from the configuration, not from the screen's.
        if (variant == "fontScale2") RuntimeEnvironment.setFontScale(2f)
        compose.setContent {
            LogroBusTheme(darkTheme = variant == "dark") {
                LanguageDialog(AppLanguage.SYSTEM, onChoose = {}, onDismiss = {})
            }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun cases(): List<Array<String>> =
            listOf("loading", "content", "unknown").flatMap { state ->
                listOf("light", "dark", "fontScale2").map { arrayOf(state, it) }
            }
    }
}
