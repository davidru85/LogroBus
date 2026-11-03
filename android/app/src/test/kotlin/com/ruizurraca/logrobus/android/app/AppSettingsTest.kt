package com.ruizurraca.logrobus.android.app

import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.logrobus.core.preferences.ThemePreference
import com.ruizurraca.logrobus.feature.settings.domain.SetTheme
import com.ruizurraca.logrobus.feature.settings.presentation.AppLanguage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

/** FR-SET-01 in the app: the theme and the language chosen in Settings apply to the whole app (LB-403, ADR-0020). */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en")
class AppSettingsTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val application = RuntimeEnvironment.getApplication()

    @After
    fun forget() {
        // AppCompat keeps both for the whole process, which Robolectric shares between tests.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }

    @Test
    fun lb403_eachThemeIsAppCompatsNightMode() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, nightModeOf(ThemePreference.SYSTEM))
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, nightModeOf(ThemePreference.LIGHT))
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, nightModeOf(ThemePreference.DARK))
    }

    @Test
    fun lb403_theSavedTheme_isTheAppsFromItsFirstFrame() {
        runBlocking { GlobalContext.get().get<SetTheme>()(ThemePreference.DARK) }

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val night = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                assertEquals(Configuration.UI_MODE_NIGHT_YES, night)
            }
        }
    }

    @Test
    fun lb403_choosingDarkInSettings_turnsTheAppDark() {
        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Settings").performClick()
        // The choices wait for Settings' first state, which reads the settings and the data status off the main thread.
        eventually { compose.onAllNodes(hasText("Dark") and isEnabled()).fetchSemanticsNodes().size == 1 }
        // On a small screen the row may sit under the navigation bar until it scrolls into view.
        compose.onNodeWithText("Dark").performScrollTo().performClick()

        eventually { AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES }
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

    @Test
    fun lb403_eachLanguageIsItsLocales_andBack() {
        assertEquals("", localesOf(AppLanguage.SYSTEM).toLanguageTags())
        assertEquals("es", localesOf(AppLanguage.SPANISH).toLanguageTags())
        assertEquals("en", localesOf(AppLanguage.ENGLISH).toLanguageTags())
        AppLanguage.entries.forEach { assertEquals(it, languageOf(localesOf(it))) }
        // A region the app does not ship still reads as its language.
        assertEquals(AppLanguage.ENGLISH, languageOf(LocaleListCompat.forLanguageTags("en-GB")))
    }

    @Test
    fun lb403_aChosenLanguage_isAndroidsAppLanguage() {
        AppCompatLanguages().set(AppLanguage.ENGLISH)

        assertEquals("en", AppCompatDelegate.getApplicationLocales().toLanguageTags())
        assertEquals(AppLanguage.ENGLISH, AppCompatLanguages().current.value)
    }

    @Test
    fun lb403_theAppTellsAndroidItsTwoLanguages_andKeepsTheChoiceOnOlderVersions() {
        assertEquals(listOf("es", "en"), locales(R.xml.locales_config))
        // Below Android 13, AppCompat keeps the chosen language itself when this service says so.
        val holder = ComponentName(application, "androidx.appcompat.app.AppLocalesMetadataHolderService")
        val flags = PackageManager.GET_META_DATA or PackageManager.MATCH_DISABLED_COMPONENTS
        val service = application.packageManager.getServiceInfo(holder, flags)
        assertEquals(true, service.metaData?.getBoolean("autoStoreLocales"))
    }

    private fun locales(xml: Int): List<String> {
        val parser = application.resources.getXml(xml)
        val names = mutableListOf<String>()
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                names += parser.getAttributeValue(ANDROID, "name")
            }
        }
        return names
    }

    private companion object {
        const val ANDROID = "http://schemas.android.com/apk/res/android"
        const val TIMEOUT_MILLIS = 5_000L
        const val POLL_MILLIS = 50L
    }
}
