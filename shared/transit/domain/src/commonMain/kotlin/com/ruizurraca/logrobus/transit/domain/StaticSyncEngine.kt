package com.ruizurraca.logrobus.transit.domain

import com.ruizurraca.logrobus.core.common.AppResult

/**
 * Keeps the cached lines and stops fresh (spec §8.5, ADR-0008). There is no seed: the first use downloads them, and
 * app start and the daily background job refresh them once they are older than 24 hours.
 */
interface StaticSyncEngine {
    /**
     * Downloads lines and stops if the cache is empty or stale, and replaces them in one transaction. [AppResult.Success]
     * means the cache is fresh, whether or not anything was downloaded. On [AppResult.Failure] the cache is as it was
     * (NFR-REL-03): a screen with no data shows the error with Retry (FR-ERR-05), and a background refresh stays
     * silent and is retried at the next trigger (FR-ERR-04).
     */
    suspend fun syncIfStale(): AppResult<Unit>
}
