package com.ruizurraca.logrobus.feature.settings.data

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.LogLevel
import com.ruizurraca.logrobus.core.common.Logger
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** FR-SET-01 on Android: "Clear cache" empties the cache directory and nothing else. */
class CacheDirectoryCachesTest {
    private val app = Files.createTempDirectory("app").toFile()
    private val cache = File(app, "cache").apply { mkdirs() }
    private val logger =
        object : Logger {
            override fun log(
                level: LogLevel,
                tag: String,
                message: String,
                throwable: Throwable?,
            ) = Unit
        }

    @Test
    fun lb403_clearCache_emptiesTheCacheDirectory_andKeepsTheDatabaseAndSettings() =
        runTest {
            File(cache, "image_cache/0a1b").apply { parentFile.mkdirs() }.writeText("image")
            File(cache, "response").writeText("live")
            val database = File(app, "databases/logrobus.db").apply { parentFile.mkdirs() }.apply { writeText("db") }
            val settings = File(app, "files/datastore/settings.preferences_pb").apply { parentFile.mkdirs() }
            settings.writeText("settings")

            val result = CacheDirectoryCaches(cache, StandardTestDispatcher(testScheduler), logger).clear()

            assertEquals(AppResult.Success(Unit), result)
            assertEquals(emptyList(), cache.list().orEmpty().toList())
            assertTrue(cache.isDirectory)
            assertEquals("db", database.readText())
            assertEquals("settings", settings.readText())
        }

    @Test
    fun lb403_anEmptyOrMissingCacheDirectory_isAlreadyClear() =
        runTest {
            val io = StandardTestDispatcher(testScheduler)

            assertEquals(AppResult.Success(Unit), CacheDirectoryCaches(cache, io, logger).clear())
            assertEquals(AppResult.Success(Unit), CacheDirectoryCaches(File(app, "missing"), io, logger).clear())
        }
}
