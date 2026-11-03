package com.ruizurraca.logrobus.feature.settings.domain

import com.ruizurraca.logrobus.core.common.AppResult

/**
 * The app's caches on the device: what each platform keeps in its cache directory, such as images. Favourites, places,
 * recents and the static data live in the database, which is never a cache to clear (FR-SET-01).
 */
interface AppCaches {
    /** Empties the caches, or fails with `Data` when the platform cannot. */
    suspend fun clear(): AppResult<Unit>
}

/** FR-SET-01: Settings' "Clear cache". */
fun interface ClearCache {
    suspend operator fun invoke(): AppResult<Unit>
}

class DefaultClearCache(
    private val caches: AppCaches,
) : ClearCache {
    override suspend fun invoke(): AppResult<Unit> = caches.clear()
}
