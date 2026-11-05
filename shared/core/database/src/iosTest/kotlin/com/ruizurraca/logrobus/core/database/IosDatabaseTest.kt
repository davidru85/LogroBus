package com.ruizurraca.logrobus.core.database

import co.touchlab.sqliter.DatabaseFileContext
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

/** ADR-0013 on iOS: the app's database lives in a file and keeps its data, its search index included. */
class IosDatabaseTest {
    private val name = "lb007-test.db"

    // Before as well as after: a file left by a crashed run would hold stop 38 already.
    @BeforeTest
    @AfterTest
    fun deleteTheFile() = DatabaseFileContext.deleteDatabase(name)

    @Test
    fun lb007_iosKeepsTheDatabaseInAFileAcrossLaunches() {
        logroBusDriver(name).run {
            insertStop(38, "Vara de Rey")
            close()
        }

        val reopened = logroBusDriver(name)

        assertEquals(listOf(38L), logroBusDatabase(reopened).searchStops("vara").map { it.id })
        reopened.close()
    }

    @Test
    fun lb407_iosBacksTheDatabaseUp() {
        // FR-FAV-03: iCloud and device backups keep an app's Application Support, except what is excluded from them,
        // a file or a folder above it; they never keep Caches or tmp. The database holds the favourites, places and
        // recents. Its file exists once something is written.
        logroBusDriver(name).run {
            logroBusDatabase(this).favouriteQueries.insertFavourite("1", FavouriteTypes.LINE, "3", Instant.DISTANT_PAST)
            close()
        }

        val file = NSURL.fileURLWithPath(DatabaseFileContext.databasePath(name, null))
        val support = applicationSupport()

        assertTrue(NSFileManager.defaultManager.fileExistsAtPath(file.path!!), "The database is not at ${file.path}")
        assertTrue(file.path!!.startsWith("$support/"), "${file.path} is not in Application Support, $support")
        // The file and the folders the app makes for it. Application Support itself is not checked: these tests run
        // outside an app's container, in the simulator's system folders, which iOS leaves out of backups.
        generateSequence(file) { it.URLByDeletingLastPathComponent }
            .takeWhile { it.path!!.startsWith("$support/") }
            .forEach { assertFalse(it.isExcludedFromBackup(), "${it.path} is excluded from backups") }
    }

    private fun applicationSupport(): String {
        val urls = NSFileManager.defaultManager.URLsForDirectory(NSApplicationSupportDirectory, NSUserDomainMask)
        return (urls.first() as NSURL).path!!
    }

    // Foundation's resourceValuesForKeys takes an error pointer, which is foreign API.
    @OptIn(ExperimentalForeignApi::class)
    private fun NSURL.isExcludedFromBackup(): Boolean {
        val values = resourceValuesForKeys(listOf(NSURLIsExcludedFromBackupKey), null)
        return (values?.get(NSURLIsExcludedFromBackupKey) as NSNumber).boolValue
    }
}
