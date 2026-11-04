package com.ruizurraca.logrobus.feature.settings.domain

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** FR-SET-01: "Clear cache" empties the app's caches, and says when it could not. */
class ClearCacheTest {
    private val caches = FakeAppCaches()

    @Test
    fun lb403_clearCache_emptiesTheAppsCaches() =
        runTest {
            assertEquals(AppResult.Success(Unit), DefaultClearCache(caches)())
            assertEquals(1, caches.clears)
        }

    @Test
    fun lb403_aCacheThatCannotBeCleared_isAFailure() =
        runTest {
            caches.failing = true

            assertEquals(AppResult.Failure(AppError.Data), DefaultClearCache(caches)())
        }
}
