package com.ruizurraca.logrobus.android.app

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.core.os.LocaleListCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.logrobus.feature.settings.presentation.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.xmlpull.v1.XmlPullParser

/** FR-SET-01 and ADR-0020: the app's languages, as AppCompat and Android see them. */
@RunWith(AndroidJUnit4::class)
class AppLanguagesTest {
    private val application = RuntimeEnvironment.getApplication()

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
    }
}
