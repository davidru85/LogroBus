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

    // Foundation's resourceValuesForKeys takes an error pointer, which is foreign API.
    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun lb407_iosBacksTheDatabaseUp() {
        // FR-FAV-03: iCloud and device backups keep Application Support, except a file excluded from them; they never
        // keep Caches or tmp. The database holds the favourites, places and recent searches.
        logroBusDriver(name).close()

        val file = NSURL.fileURLWithPath(DatabaseFileContext.databasePath(name, null))
        val support = NSFileManager.defaultManager.URLsForDirectory(NSApplicationSupportDirectory, NSUserDomainMask)
        val values = file.resourceValuesForKeys(listOf(NSURLIsExcludedFromBackupKey), null)
        val excluded = (values?.get(NSURLIsExcludedFromBackupKey) as NSNumber).boolValue
        val supportPath = (support.first() as NSURL).path!!

        assertTrue(file.path!!.startsWith(supportPath), "${file.path} is not in Application Support, $supportPath")
        assertFalse(excluded, "${file.path} is excluded from backups")
    }
}
