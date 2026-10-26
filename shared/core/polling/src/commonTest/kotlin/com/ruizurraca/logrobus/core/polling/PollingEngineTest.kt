package com.ruizurraca.logrobus.core.polling

import app.cash.turbine.test
import com.ruizurraca.logrobus.core.common.AppLifecycle
import com.ruizurraca.logrobus.core.common.AppResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Spec §8.6, FR-ERR-03 and NFR-PERF-07: one shared engine polls live data for the visible screens. */
@OptIn(ExperimentalCoroutinesApi::class) // currentTime
class PollingEngineTest {
    private class FakeAppLifecycle : AppLifecycle {
        override val isInForeground = MutableStateFlow(true)
    }

    private val appLifecycle = FakeAppLifecycle()

    /** When each fetch ran, in virtual time since the test started. */
    private val fetchTimes = mutableListOf<Duration>()

    private fun TestScope.engine(
        random: Random = Random(SEED),
        maxJitter: Duration = Duration.ZERO,
    ) = DefaultPollingEngine(
        scope = backgroundScope,
        appLifecycle = appLifecycle,
        clock = { START + currentTime.milliseconds },
        random = random,
        maxJitter = maxJitter,
    )

    /** A fetch that records its time and returns [result] for the n-th call, counting from 1. */
    private fun TestScope.recordingFetch(
        result: (Int) -> AppResult<Int> = { AppResult.Success(it) },
    ): suspend () -> AppResult<Int> =
        {
            fetchTimes += currentTime.milliseconds
            result(fetchTimes.size)
        }

    @Test
    fun lb009_pollsAtTheIntervalOnlyWhileCollected() =
        runTest {
            val arrivals = engine().poll(ARRIVALS, 20.seconds, recordingFetch())
            advanceTimeBy(1.minutes)
            assertEquals(emptyList(), fetchTimes)

            arrivals.test {
                assertEquals(Timed(1, START + 1.minutes, lastError = null), awaitItem())
                assertEquals(Timed(2, START + 80.seconds, lastError = null), awaitItem())
            }
            advanceTimeBy(1.minutes)

            assertEquals(listOf(1.minutes, 80.seconds), fetchTimes)
        }

    private companion object {
        const val SEED = 2026
        val START = Instant.parse("2026-10-26T08:00:00Z")
        val ARRIVALS = PollKey("arrivals:152")
    }
}
