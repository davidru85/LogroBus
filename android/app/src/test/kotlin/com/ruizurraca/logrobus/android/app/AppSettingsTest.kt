package com.ruizurraca.logrobus.android.app

import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.logrobus.core.preferences.ThemePreference
import com.ruizurraca.logrobus.feature.settings.domain.SetTheme
import com.ruizurraca.logrobus.feature.settings.presentation.AppLanguage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
        compose.onNodeWithText("Dark").performClick()

        compose.waitUntil(TIMEOUT_MILLIS) { AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES }
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
    }
}
