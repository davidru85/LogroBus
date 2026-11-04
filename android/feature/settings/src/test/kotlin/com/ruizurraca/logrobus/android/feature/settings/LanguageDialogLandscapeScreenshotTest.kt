package com.ruizurraca.logrobus.android.feature.settings

import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import com.ruizurraca.logrobus.android.core.designsystem.LogroBusTheme
import com.ruizurraca.logrobus.feature.settings.presentation.AppLanguage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** NFR-A11Y: the language dialog on a phone in landscape at 200 % font, where its languages scroll (LB-403). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "es-w640dp-h360dp-land", sdk = [SCREENSHOT_SDK])
class LanguageDialogLandscapeScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun lb403_languageDialog_inLandscapeAtLargeText_screenshot() {
        // A dialog is a window of its own, whose density comes from the configuration, not from the screen's.
        RuntimeEnvironment.setFontScale(2f)
        compose.setContent {
            LogroBusTheme { LanguageDialog(AppLanguage.SYSTEM, onChoose = {}, onDismiss = {}) }
        }

        compose
            .onNode(
                isDialog(),
            ).captureRoboImage("src/test/screenshots/Settings_languageDialog_landscape_fontScale2.png")
    }
}
