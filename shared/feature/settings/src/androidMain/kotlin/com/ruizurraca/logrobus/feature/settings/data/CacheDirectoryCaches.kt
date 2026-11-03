package com.ruizurraca.logrobus.feature.settings.data

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.feature.settings.domain.AppCaches
import kotlinx.coroutines.CoroutineDispatcher
import java.io.File

/** Android's caches: everything in the app's cache [directory], `Context.cacheDir` (FR-SET-01). */
class CacheDirectoryCaches(
    @Suppress("unused") private val directory: File,
    @Suppress("unused") private val io: CoroutineDispatcher,
    @Suppress("unused") private val logger: Logger,
) : AppCaches {
    override suspend fun clear(): AppResult<Unit> = AppResult.Success(Unit)
}
