package com.ruizurraca.logrobus.feature.settings.data

import android.content.Context
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.warn
import com.ruizurraca.logrobus.feature.settings.domain.AppCaches
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File

/** The app's caches on Android: its cache directory, `Context.cacheDir`. */
fun appCaches(
    context: Context,
    io: CoroutineDispatcher,
    logger: Logger,
): AppCaches = CacheDirectoryCaches(context.cacheDir, io, logger)

/**
 * Android's caches: everything in the app's cache [directory], `Context.cacheDir` (FR-SET-01), where image caches will
 * live, and any HTTP cache a library keeps; the source client keeps none (`architecture.md` §4.1). The database and
 * the settings are in other directories, so they stay.
 */
internal class CacheDirectoryCaches(
    private val directory: File,
    private val io: CoroutineDispatcher,
    private val logger: Logger,
) : AppCaches {
    override suspend fun clear(): AppResult<Unit> =
        withContext(io) {
            val left = directory.listFiles().orEmpty().count { !it.deleteRecursively() }
            if (left == 0) {
                AppResult.Success(Unit)
            } else {
                logger.warn(TAG, "Clearing the cache left $left entries")
                AppResult.Failure(AppError.Data)
            }
        }

    private companion object {
        const val TAG = "Caches"
    }
}
