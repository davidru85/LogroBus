package com.ruizurraca.logrobus.feature.lines.domain

import app.cash.turbine.test
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.transit.domain.Line
import com.ruizurraca.logrobus.transit.domain.LineKind
import com.ruizurraca.logrobus.transit.domain.LinesRepository
import com.ruizurraca.logrobus.transit.domain.StaticSyncEngine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** LB-101 and ADR-0008: the lines come from the cache, which the first use fills; there is no seed. */
class ObserveLinesTest {
    private val line1 = line(1, "1")
    private val line2 = line(2, "2")
    private val cache = MutableStateFlow<AppResult<List<Line>>>(AppResult.Success(emptyList()))
    private val sync = ScriptedSync()
    private val observeLines =
        DefaultObserveLines(
            sync,
            object : LinesRepository {
                override fun observeLines() = cache
            },
        )

    @Test
    fun lb101_cachedLinesShowWhileTheSyncRuns() =
        runTest {
            cache.value = AppResult.Success(listOf(line1))

            observeLines().test {
                assertEquals(AppResult.Success(listOf(line1)), awaitItem())
                expectNoEvents()
            }
        }

    @Test
    fun lb101_anEmptyCacheWaitsForTheFirstSync() =
        runTest {
            observeLines().test {
                expectNoEvents()
                sync.complete(AppResult.Success(Unit), stores = listOf(line1, line2))
                assertEquals(AppResult.Success(listOf(line1, line2)), awaitItem())
            }
        }

    @Test
    fun lb101_aFirstLaunchWithNoNetworkGivesTheNetworkError() =
        runTest {
            observeLines().test {
                sync.complete(AppResult.Failure(AppError.Network))
                assertEquals(AppResult.Failure(AppError.Network), awaitItem())
            }
        }

    @Test
    fun lb101_aFailedRefreshKeepsTheCachedLines_silently() =
        runTest {
            // FR-ERR-04: a refresh that fails says nothing while there is something to show.
            cache.value = AppResult.Success(listOf(line1))

            observeLines().test {
                assertEquals(AppResult.Success(listOf(line1)), awaitItem())
                sync.complete(AppResult.Failure(AppError.Network))
                expectNoEvents()
            }
        }

    @Test
    fun lb101_aSyncThatStoresNoLinesGivesNoLines() =
        runTest {
            observeLines().test {
                sync.complete(AppResult.Success(Unit))
                assertEquals(AppResult.Success(emptyList()), awaitItem())
            }
        }

    @Test
    fun lb101_aCacheThatCannotBeReadGivesItsFailure() =
        runTest {
            cache.value = AppResult.Failure(AppError.Data)

            observeLines().test {
                sync.complete(AppResult.Success(Unit))
                assertEquals(AppResult.Failure(AppError.Data), awaitItem())
            }
        }

    @Test
    fun lb101_retryingSyncsAgain_andLoadsTheLinesOnceTheNetworkIsBack() =
        runTest {
            observeLines().test {
                sync.complete(AppResult.Failure(AppError.Network))
                assertEquals(AppResult.Failure(AppError.Network), awaitItem())
            }

            // Retry collects the use case again.
            observeLines().test {
                sync.complete(AppResult.Success(Unit), stores = listOf(line1))
                assertEquals(AppResult.Success(listOf(line1)), awaitItem())
            }
            assertEquals(2, sync.calls)
        }

    /** A sync that waits for [complete], which stores its lines in the cache before it returns, as the engine does. */
    private inner class ScriptedSync : StaticSyncEngine {
        private var result = CompletableDeferred<Pair<AppResult<Unit>, List<Line>?>>()
        var calls = 0
            private set

        fun complete(
            result: AppResult<Unit>,
            stores: List<Line>? = null,
        ) {
            this.result.complete(result to stores)
        }

        override suspend fun syncIfStale(): AppResult<Unit> {
            calls++
            val (outcome, stored) = result.await()
            result = CompletableDeferred()
            stored?.let { cache.value = AppResult.Success(it) }
            return outcome
        }
    }

    private fun line(
        id: Int,
        code: String,
    ) = Line(id, code, "Line $code", color = 0xFF0000FF, onColor = 0xFFFFFFFF, LineKind.DAY, isOperational = true)
}
