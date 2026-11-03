package com.ruizurraca.logrobus.feature.settings.data

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.LogLevel
import com.ruizurraca.logrobus.core.common.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUUID
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.stringByDeletingLastPathComponent
import platform.Foundation.writeToFile
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** FR-SET-01 on iOS: "Clear cache" empties the caches directory and nothing else. */
@OptIn(ExperimentalForeignApi::class)
class CachesDirectoryCachesTest {
    private val files = NSFileManager.defaultManager
    private val app = NSTemporaryDirectory().path("app-${NSUUID().UUIDString}")
    private val caches = app.path("Library/Caches")
    private val logger =
        object : Logger {
            override fun log(
                level: LogLevel,
                tag: String,
                message: String,
                throwable: Throwable?,
            ) = Unit
        }

    @AfterTest
    fun delete() {
        files.removeItemAtPath(app, null)
    }

    @Test
    fun lb403_clearCache_emptiesTheCachesDirectory_andKeepsTheDatabase() =
        runTest {
            write(caches.path("images/0a1b"), "image")
            write(caches.path("response"), "live")
            val database = app.path("Library/Application Support/databases/logrobus.db")
            write(database, "db")

            val result = CachesDirectoryCaches(caches, emptySet(), StandardTestDispatcher(testScheduler), logger).clear()

            assertEquals(AppResult.Success(Unit), result)
            assertEquals(emptyList(), files.contentsOfDirectoryAtPath(caches, null).orEmpty())
            assertTrue(files.fileExistsAtPath(database))
        }

    @Test
    fun lb403_aMissingCachesDirectory_isAlreadyClear() =
        runTest {
            val io = StandardTestDispatcher(testScheduler)

            assertEquals(
                AppResult.Success(Unit),
                CachesDirectoryCaches(app.path("missing"), emptySet(), io, logger).clear(),
            )
        }

    @Test
    fun lb403_theUrlCachesOwnFolder_isLeftForTheUrlCacheToEmpty() =
        runTest {
            // The system's URL cache keeps its database open in a folder named after the app's bundle id.
            write(caches.path("com.ruizurraca.logrobus/Cache.db"), "open database")
            write(caches.path("images/0a1b"), "image")

            val kept = setOf("com.ruizurraca.logrobus")
            CachesDirectoryCaches(caches, kept, StandardTestDispatcher(testScheduler), logger).clear()

            assertEquals(listOf<Any?>("com.ruizurraca.logrobus"), files.contentsOfDirectoryAtPath(caches, null))
        }

    private fun write(
        path: String,
        text: String,
    ) {
        files.createDirectoryAtPath(path.parent(), withIntermediateDirectories = true, attributes = null, error = null)
        @Suppress("CAST_NEVER_SUCCEEDS")
        (text as NSString).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }
}

@Suppress("CAST_NEVER_SUCCEEDS")
private fun String.path(component: String): String = (this as NSString).stringByAppendingPathComponent(component)

@Suppress("CAST_NEVER_SUCCEEDS")
private fun String.parent(): String = (this as NSString).stringByDeletingLastPathComponent
