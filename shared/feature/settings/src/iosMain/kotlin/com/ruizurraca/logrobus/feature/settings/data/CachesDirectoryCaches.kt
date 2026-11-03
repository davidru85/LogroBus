package com.ruizurraca.logrobus.feature.settings.data

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.feature.settings.domain.AppCaches
import kotlinx.coroutines.CoroutineDispatcher

/**
 * iOS's caches: everything in the app's caches [directory], `Library/Caches` (FR-SET-01), but the entries [ownedByUrlCache],
 * which the system's URL cache keeps open and empties itself.
 */
class CachesDirectoryCaches(
    @Suppress("unused") private val directory: String,
    @Suppress("unused") private val ownedByUrlCache: Set<String>,
    @Suppress("unused") private val io: CoroutineDispatcher,
    @Suppress("unused") private val logger: Logger,
) : AppCaches {
    override suspend fun clear(): AppResult<Unit> = AppResult.Success(Unit)
}
