package com.ruizurraca.logrobus.android.app

import android.content.pm.ApplicationInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.preferences.PreferencesStore
import com.ruizurraca.logrobus.core.preferences.ThemePreference
import com.ruizurraca.logrobus.favourites.domain.FavouriteLinesRepository
import com.ruizurraca.logrobus.umbrella.initKoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.robolectric.RuntimeEnvironment
import org.w3c.dom.Element
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory

/**
 * FR-FAV-03 (LB-407): the OS's backups keep the favourites and the settings, so a new device gets them back. Android
 * backs an app up to the user's Google account (Auto Backup), and copies it in a transfer to a new device; from
 * Android 12 by the data extraction rules, before it by the full backup content rules. Each test saves through the
 * app's graph, then restores as a new device would, keeping only the files the rules back up, and opens the app again.
 */
@RunWith(AndroidJUnit4::class)
class BackupTest {
    private val application = RuntimeEnvironment.getApplication()

    @Test
    fun lb407_theManifestGivesTheBackupRules() {
        val info = application.packageManager.getApplicationInfo(application.packageName, 0)

        assertTrue("Backups are allowed", info.flags and ApplicationInfo.FLAG_ALLOW_BACKUP != 0)
        assertNotEquals("Android 12+ has data extraction rules", 0, manifestRules("dataExtractionRules"))
        assertNotEquals("Android 8 to 11 have full backup content rules", 0, manifestRules("fullBackupContent"))
    }

    @Test
    fun lb407_aCloudBackupRestoresTheFavouritesAndTheSettings() = assertRestoresTheUsersData(cloudBackup())

    @Test
    fun lb407_aTransferToANewDeviceRestoresTheFavouritesAndTheSettings() = assertRestoresTheUsersData(deviceTransfer())

    @Test
    fun lb407_aBackupBeforeAndroid12RestoresTheFavouritesAndTheSettings() = assertRestoresTheUsersData(fullBackup())

    @Test
    fun lb407_backupsLeaveOutWhatCanBeDownloadedAgain() {
        // A map's tile cache, for one, which MapLibre keeps in the files directory: it would fill the 25 MB that Auto
        // Backup allows an app, and then nothing would be backed up.
        val tiles = File(application.filesDir, "mbgl-offline.db").apply { writeText("tiles") }

        listOf(cloudBackup(), deviceTransfer(), fullBackup()).forEach { rules ->
            assertFalse("${rules.name} backs a tile cache up", rules.backsUp(tiles))
        }
    }

    @Test
    fun lb407_belowAndroid13TheLanguageChosenInSettingsIsRestored() {
        // AppCompat keeps the app's language below Android 13 in this file (ADR-0020); from Android 13 the system
        // keeps it, and backs it up itself.
        val language = File(application.filesDir, APP_COMPAT_LOCALES_FILE).apply { writeText("en") }

        listOf(cloudBackup(), deviceTransfer(), fullBackup()).forEach { rules ->
            assertTrue("${rules.name} leaves out the language", rules.backsUp(language))
        }
    }

    private fun assertRestoresTheUsersData(rules: BackupRules) {
        runBlocking {
            assertTrue(GlobalContext.get().get<FavouriteLinesRepository>().add(LINE) is AppResult.Success)
            assertTrue(GlobalContext.get().get<PreferencesStore>().setTheme(ThemePreference.DARK) is AppResult.Success)
        }
        // The app's process ends before a backup, which closes the database and the settings file.
        stopKoin()

        restoreOnly(rules)
        initKoin(application, appVersion = "1.0.0", debug = true)

        runBlocking {
            val favourites =
                GlobalContext
                    .get()
                    .get<FavouriteLinesRepository>()
                    .observeFavouriteLineIds()
                    .first()
            assertEquals("${rules.name}: the favourite lines", AppResult.Success(listOf(LINE)), favourites)
            val preferences =
                GlobalContext
                    .get()
                    .get<PreferencesStore>()
                    .preferences
                    .first()
            assertEquals("${rules.name}: the theme", ThemePreference.DARK, preferences.theme)
        }
    }

    /** Deletes every file of the app that [rules] do not back up, which a new device would not get. */
    private fun restoreOnly(rules: BackupRules) {
        application.dataDir
            .walkBottomUp()
            .filter { it.isFile && !rules.backsUp(it) }
            .forEach { it.delete() }
    }

    private fun cloudBackup() = rules("cloud backup", manifestRules("dataExtractionRules"), "cloud-backup")

    private fun deviceTransfer() = rules("device transfer", manifestRules("dataExtractionRules"), "device-transfer")

    private fun fullBackup() = rules("full backup", manifestRules("fullBackupContent"), section = null)

    /**
     * The XML resource that the application's [attribute] names in the merged manifest, or 0. It is read from the
     * manifest that the build gives Robolectric, since Robolectric's parser predates Android 12 and leaves the
     * application's data extraction rules empty.
     */
    private fun manifestRules(attribute: String): Int {
        val config =
            Properties().apply {
                BackupTest::class.java.classLoader!!
                    .getResourceAsStream(TEST_CONFIG)!!
                    .use(::load)
            }
        val manifest =
            DocumentBuilderFactory
                .newInstance()
                .apply { isNamespaceAware = true }
                .newDocumentBuilder()
                .parse(File(config.getProperty("android_merged_manifest")))
        val app = manifest.getElementsByTagName("application").item(0) as Element
        val name = app.getAttributeNS(ANDROID, attribute).removePrefix("@xml/")
        return if (name.isEmpty()) 0 else application.resources.getIdentifier(name, "xml", application.packageName)
    }

    /** The include and exclude rules of the XML resource [xml], in its element [section], or at its root if null. */
    private fun rules(
        name: String,
        xml: Int,
        section: String?,
    ): BackupRules {
        assertTrue("The manifest names no rules for the $name", xml > 0)
        val parser = application.resources.getXml(xml)
        val includes = mutableListOf<File>()
        val excludes = mutableListOf<File>()
        var inSection = section == null
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            val opens = parser.eventType == XmlPullParser.START_TAG
            when {
                section != null && parser.name == section -> inSection = opens
                opens && inSection && parser.name == "include" -> includes += parser.ruleFile()
                opens && inSection && parser.name == "exclude" -> excludes += parser.ruleFile()
            }
        }
        return BackupRules(name, includes, excludes, neverBackedUp)
    }

    /** The file or directory a rule names, in its domain. */
    private fun XmlPullParser.ruleFile(): File {
        val domain =
            when (val domain = getAttributeValue(null, "domain")) {
                "root" -> application.dataDir
                "file" -> application.filesDir
                "database" -> application.getDatabasePath("any").parentFile!!
                "sharedpref" -> File(application.dataDir, "shared_prefs")
                else -> error("A backup rule's domain this test does not know: $domain")
            }
        return File(domain, getAttributeValue(null, "path")).normalize()
    }

    /** The directories Android never backs up, whatever the rules say. */
    private val neverBackedUp
        get() = listOf(application.cacheDir, application.codeCacheDir, application.noBackupFilesDir)

    private class BackupRules(
        val name: String,
        private val includes: List<File>,
        private val excludes: List<File>,
        private val neverBackedUp: List<File>,
    ) {
        /** With no include, every file is backed up; with includes, only the files and directories they name. */
        fun backsUp(file: File): Boolean {
            val path = file.normalize()
            return neverBackedUp.none { path.startsWith(it) } &&
                (includes.isEmpty() || includes.any { path.startsWith(it) }) &&
                excludes.none { path.startsWith(it) }
        }
    }

    private companion object {
        const val LINE = 3

        /** Where the build tells Robolectric about the app, its merged manifest included. */
        const val TEST_CONFIG = "com/android/tools/test_config.properties"

        const val ANDROID = "http://schemas.android.com/apk/res/android"

        /** AppCompat's own name for it (`AppLocalesStorageHelper`). */
        const val APP_COMPAT_LOCALES_FILE = "androidx.appcompat.app.AppCompatDelegate.application_locales_record_file"
    }
}
