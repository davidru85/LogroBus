package com.ruizurraca.logrobus.android.app

import android.content.pm.ApplicationInfo
import androidx.core.app.AppLocalesStorageHelper
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

/**
 * FR-FAV-03 (LB-407): the OS's backups keep the favourites and the settings, so a new device gets them back. Android
 * backs an app up to the user's Google account (Auto Backup), and copies it in a transfer to a new device; from
 * Android 12 by the data extraction rules, before it by the full backup content rules. Each test saves through the
 * app, copies the files the rules back up, restores them to an app with no data, as on a new device, and opens the
 * app again.
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
        // Below Android 13, AppCompat keeps the language chosen in Settings in a file of the app's (ADR-0020), which
        // it writes and reads through androidx.core's storage helper. From Android 13 the system keeps it and backs it
        // up itself.
        listOf(cloudBackup(), deviceTransfer(), fullBackup()).forEach { rules ->
            AppLocalesStorageHelper.persistLocales(application, "en")

            restoreOnANewDevice(backUp(rules))

            assertEquals("${rules.name}: the language", "en", AppLocalesStorageHelper.readLocales(application))
        }
    }

    private fun assertRestoresTheUsersData(rules: BackupRules) {
        runBlocking {
            assertTrue(GlobalContext.get().get<FavouriteLinesRepository>().add(LINE) is AppResult.Success)
            assertTrue(GlobalContext.get().get<PreferencesStore>().setTheme(ThemePreference.DARK) is AppResult.Success)
        }
        val tiles = File(application.filesDir, "mbgl-offline.db").apply { writeText("tiles") }

        // A backup copies the files as they are on disk, with the app still running in the background, so the last
        // changes may still be in the database's journal or write-ahead log.
        restoreOnANewDevice(backUp(rules))
        initKoin(application, appVersion = "1.0.0", debug = true)

        assertFalse("${rules.name}: a tile cache came back", tiles.exists())

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

    /** A copy of every file of the app that [rules] back up, by its path in the app's data directory. */
    private fun backUp(rules: BackupRules): Map<String, ByteArray> =
        application.dataDir
            .walk()
            .filter { it.isFile && rules.backsUp(it) }
            .associate { it.relativeTo(application.dataDir).path to it.readBytes() }
            .also { assertTrue("${rules.name} backed nothing up", it.isNotEmpty()) }

    /** The app, stopped, gets [backup] back with no other data, as on a new device. */
    private fun restoreOnANewDevice(backup: Map<String, ByteArray>) {
        stopKoin()
        application.dataDir
            .listFiles()
            .orEmpty()
            .forEach { it.deleteRecursively() }
        backup.forEach { (path, bytes) ->
            File(application.dataDir, path).apply { parentFile!!.mkdirs() }.writeBytes(bytes)
        }
    }

    private fun cloudBackup() = rules("cloud backup", manifestRules("dataExtractionRules"), "cloud-backup")

    private fun deviceTransfer() = rules("device transfer", manifestRules("dataExtractionRules"), "device-transfer")

    private fun fullBackup() = rules("full backup", manifestRules("fullBackupContent"), section = null)

    /** The XML resource that the application's [attribute] names in the merged manifest, or 0. */
    private fun manifestRules(attribute: String): Int {
        val app = MergedManifest.document.getElementsByTagName("application").item(0) as Element
        val name = app.getAttributeNS(MergedManifest.ANDROID, attribute).removePrefix("@xml/")
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
                opens && inSection && parser.name == "include" -> includes += parser.included()
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

    /**
     * The files an include names. Android adds a database file's journal and write-ahead log to its include itself
     * (`FullBackup.BackupScheme`), so the rules name the database only.
     */
    private fun XmlPullParser.included(): List<File> {
        val file = ruleFile()
        return if (getAttributeValue(null, "domain") == "database") {
            listOf(file, File("${file.path}-journal"), File("${file.path}-wal"))
        } else {
            listOf(file)
        }
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
    }
}
