package com.ruizurraca.logrobus.android.app

import android.app.UiModeManager
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.os.LocaleListCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.logrobus.android.feature.settings.AppLanguages
import com.ruizurraca.logrobus.feature.settings.presentation.AppLanguage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * FR-SET-01 in the app, end to end: the theme and the language chosen in Settings apply to the whole app (LB-403,
 * ADR-0020). Robolectric runs the target Android, so the theme goes to the system's per-app night mode.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en")
class AppSettingsTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val application = RuntimeEnvironment.getApplication()

    @After
    fun forget() {
        // AppCompat keeps both for the whole process, which Robolectric shares between tests.
        forgetAppCompatsNightMode()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }

    @Test
    fun lb403_choosingDarkInSettings_turnsTheAppDark_andSettingsStays() {
        openSettings()
        compose.onNodeWithText("Dark").performScrollTo().performClick()

        val uiModes = application.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        eventually { shadowOf(uiModes).applicationNightMode == UiModeManager.MODE_NIGHT_YES }
        compose.onNode(hasText("Settings") and isHeading()).assertIsDisplayed()
    }

    @Test
    fun lb403_aChosenLanguage_isAndroidsAppLanguage() {
        AppCompatLanguages().set(AppLanguage.ENGLISH)

        assertEquals("en", AppCompatDelegate.getApplicationLocales().toLanguageTags())
        assertEquals(AppLanguage.ENGLISH, AppCompatLanguages().current.value)
    }

    @Test
    fun lb403_aLanguageChosenInAndroidsOwnSettings_showsInSettings() {
        // From Android 13 the system's settings offer the app's languages too (localeConfig). A change there
        // recreates the activity, while the process and its graph live on.
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("es"))
        compose.activityRule.scenario.recreate()

        assertEquals(
            AppLanguage.SPANISH,
            GlobalContext
                .get()
                .get<AppLanguages>()
                .current.value,
        )
    }

    private fun openSettings() {
        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Settings").performClick()
        // The choices wait for Settings' first state, which reads the settings and the data status off the main thread.
        eventually { compose.onAllNodes(hasText("Dark") and isEnabled()).fetchSemanticsNodes().size == 1 }
    }

    /**
     * Waits in real time for [condition]: the settings are saved on DataStore's own thread, which Compose's test clock,
     * virtual under Robolectric, does not wait for.
     */
    private fun eventually(condition: () -> Boolean) {
        var polls = 0
        while (!condition() && polls++ < TIMEOUT_MILLIS / POLL_MILLIS) {
            Thread.sleep(POLL_MILLIS)
            compose.waitForIdle()
        }
        assertTrue("Still not so after $TIMEOUT_MILLIS ms", condition())
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
        const val POLL_MILLIS = 50L
    }
}
