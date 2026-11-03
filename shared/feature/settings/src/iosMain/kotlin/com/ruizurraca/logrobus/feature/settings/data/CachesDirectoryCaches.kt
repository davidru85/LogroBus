package com.ruizurraca.logrobus.feature.settings.data

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.warn
import com.ruizurraca.logrobus.feature.settings.domain.AppCaches
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import platform.Foundation.NSBundle
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSURLCache
import platform.Foundation.NSUserDomainMask
import platform.Foundation.stringByAppendingPathComponent

/**
 * The app's caches on iOS: its `Library/Caches`, where the system's URL cache keeps a folder named after the app's
 * bundle id.
 */
fun appCaches(
    io: CoroutineDispatcher,
    logger: Logger,
): AppCaches =
    CachesDirectoryCaches(
        directory = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).first() as String,
        ownedByUrlCache = setOfNotNull(NSBundle.mainBundle.bundleIdentifier),
        io = io,
        logger = logger,
    )

/**
 * iOS's caches: everything in the app's caches [directory], `Library/Caches` (FR-SET-01), where image caches will live,
 * and any HTTP cache a system library keeps; the source client keeps none (`architecture.md` §4.1). The entries
 * [ownedByUrlCache], the folder the system's URL cache keeps its open database in, are left to the URL cache, which
 * empties itself. The database and the settings are in other directories, so they stay.
 */
@OptIn(ExperimentalForeignApi::class)
internal class CachesDirectoryCaches(
    private val directory: String,
    private val ownedByUrlCache: Set<String>,
    private val io: CoroutineDispatcher,
    private val logger: Logger,
) : AppCaches {
    override suspend fun clear(): AppResult<Unit> =
        withContext(io) {
            NSURLCache.sharedURLCache.removeAllCachedResponses()
            val files = NSFileManager.defaultManager
            val entries = files.contentsOfDirectoryAtPath(directory, null).orEmpty().filterIsInstance<String>()
            val left = (entries - ownedByUrlCache).count { !files.removeItemAtPath(directory.child(it), null) }
            if (left == 0) {
                AppResult.Success(Unit)
            } else {
                logger.warn(TAG, "Clearing the caches left $left entries")
                AppResult.Failure(AppError.Data)
            }
        }

    private companion object {
        const val TAG = "Caches"
    }
}

@Suppress("CAST_NEVER_SUCCEEDS") // A Kotlin string is an NSString on iOS.
private fun String.child(name: String): String = (this as NSString).stringByAppendingPathComponent(name)
