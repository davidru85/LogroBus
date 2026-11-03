package com.ruizurraca.logrobus.feature.settings.data

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.warn
import com.ruizurraca.logrobus.feature.settings.domain.AppCaches
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android's caches: everything in the app's cache [directory], `Context.cacheDir` (FR-SET-01), where image and HTTP
 * caches live. The database and the settings are in other directories, so they stay.
 */
class CacheDirectoryCaches(
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
