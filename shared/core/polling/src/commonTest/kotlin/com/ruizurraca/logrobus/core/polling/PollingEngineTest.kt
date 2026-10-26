package com.ruizurraca.logrobus.core.polling

import app.cash.turbine.test
import com.ruizurraca.logrobus.core.common.AppLifecycle
import com.ruizurraca.logrobus.core.common.AppResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
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

    @Test
    fun lb009_collectorsOfTheSameKeyShareOneRequestPerInterval() =
        runTest {
            val engine = engine()
            val first = mutableListOf<Int?>()
            val second = mutableListOf<Int?>()
            var vehicleFetches = 0

            backgroundScope.launch { engine.poll(ARRIVALS, 20.seconds, recordingFetch()).collect { first += it.value } }
            advanceTimeBy(30.seconds)
            // A late collector gets the latest value at once, without a request of its own.
            backgroundScope.launch { engine.poll(ARRIVALS, 20.seconds, recordingFetch()).collect { second += it.value } }
            // Another key polls on its own.
            backgroundScope.launch {
                engine.poll(VEHICLES, 10.seconds) { AppResult.Success(++vehicleFetches) }.collect {}
            }
            advanceTimeBy(30.seconds)

            assertEquals(listOf(0.seconds, 20.seconds, 40.seconds), fetchTimes)
            assertEquals(listOf<Int?>(1, 2, 3), first)
            assertEquals(listOf<Int?>(2, 3), second)
            assertEquals(3, vehicleFetches)
        }

    @Test
    fun lb009_fetchTimesFallWithinTheIntervalPlusOrMinusOneSecond() =
        runTest {
            // Spec §8.6: jitter keeps the app's users from polling the source in step.
            val engine = engine(maxJitter = POLL_JITTER)

            backgroundScope.launch { engine.poll(ARRIVALS, 20.seconds, recordingFetch()).collect {} }
            advanceTimeBy(10.minutes)

            val gaps = fetchTimes.gaps()
            assertTrue(gaps.size >= 20, "gaps: $gaps")
            assertTrue(gaps.all { it in 19.seconds..21.seconds }, "gaps: $gaps")
            assertTrue(gaps.distinct().size > 1, "no jitter: $gaps")
        }

    @Test
    fun lb009_jitterNeverTakesAWaitBelowTheTenSecondFloor() =
        runTest {
            // LB-004 review: MIN_POLL_INTERVAL applies again after the jitter, so 10 s never becomes 9 s.
            val engine = engine(maxJitter = POLL_JITTER)
            val tooFast = mutableListOf<Duration>()

            backgroundScope.launch { engine.poll(VEHICLES, 10.seconds, recordingFetch()).collect {} }
            backgroundScope.launch {
                engine.poll(PollKey("too-fast"), 5.seconds) {
                    tooFast += currentTime.milliseconds
                    AppResult.Success(0)
                }.collect {}
            }
            advanceTimeBy(10.minutes)

            val gaps = fetchTimes.gaps()
            assertTrue(gaps.all { it in 10.seconds..11.seconds }, "gaps: $gaps")
            assertEquals(10.seconds, gaps.min())
            assertTrue(tooFast.gaps().all { it >= 10.seconds }, "gaps: ${tooFast.gaps()}")
        }

    private fun List<Duration>.gaps() = zipWithNext { previous, next -> next - previous }

    private companion object {
        const val SEED = 2026
        val START = Instant.parse("2026-10-26T08:00:00Z")
        val ARRIVALS = PollKey("arrivals:152")
        val VEHICLES = PollKey("vehicles")
    }
}
