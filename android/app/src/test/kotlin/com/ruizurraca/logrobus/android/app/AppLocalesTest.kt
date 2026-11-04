package com.ruizurraca.logrobus.android.app

import android.os.LocaleList
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * LB-405 (NFR-I18N-01): the app speaks Spanish and English, and Spanish on a device that speaks neither. Its own
 * strings fall back to Spanish by themselves; the activity's locale makes Material's and Android's strings, the
 * plural rules and the date formats agree with them.
 */
@RunWith(AndroidJUnit4::class)
class AppLocalesTest {
    @Test
    fun lb405_theDevicesLanguagesThatTheAppSpeaksStay_inTheirOrder() {
        assertEquals("es-MX", supportedLocales(LocaleList.forLanguageTags("es-MX")).toLanguageTags())
        assertEquals("en-GB,es-ES", supportedLocales(LocaleList.forLanguageTags("en-GB,es-ES")).toLanguageTags())
        assertEquals("en-GB", supportedLocales(LocaleList.forLanguageTags("fr-FR,en-GB,de-DE")).toLanguageTags())
    }

    @Test
    fun lb405_aDeviceInAnyOtherLanguage_getsSpanish_inItsRegion() {
        assertEquals("es-FR", supportedLocales(LocaleList.forLanguageTags("fr-FR")).toLanguageTags())
        assertEquals("es-DE", supportedLocales(LocaleList.forLanguageTags("de-DE,it-IT")).toLanguageTags())
        assertEquals("es", supportedLocales(LocaleList.forLanguageTags("ca")).toLanguageTags())
        assertEquals("es", supportedLocales(LocaleList.getEmptyLocaleList()).toLanguageTags())
    }

    @Test
    @Config(qualifiers = "fr-rFR")
    fun lb405_onAFrenchDevice_theWholeAppIsInSpanish() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(
                    "es-FR",
                    activity.resources.configuration.locales
                        .toLanguageTags(),
                )
                // Android's own strings, as Material's, follow the activity's locale.
                assertEquals("Cancelar", activity.getString(android.R.string.cancel))
            }
        }
    }

    @Test
    @Config(qualifiers = "en-rGB")
    fun lb405_onAnEnglishDevice_theAppIsInEnglish() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(
                    "en-GB",
                    activity.resources.configuration.locales
                        .toLanguageTags(),
                )
                assertEquals("Cancel", activity.getString(android.R.string.cancel))
            }
        }
    }
}
