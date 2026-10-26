package com.ruizurraca.logrobus.core.polling

import app.cash.turbine.test
import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppLifecycle
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.LogLevel
import com.ruizurraca.logrobus.core.common.Logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Spec §8.6, FR-ERR-03 and NFR-PERF-07: one shared engine polls live data for the visible screens. */
@OptIn(ExperimentalCoroutinesApi::class) // currentTime, runCurrent
class PollingEngineTest {
    private class FakeAppLifecycle : AppLifecycle {
        override val isInForeground = MutableStateFlow(true)
    }

    private val appLifecycle = FakeAppLifecycle()

    /** What the engine logged: level, tag, message and throwable. */
    private val logged = mutableListOf<List<Any?>>()
    private val logger =
        object : Logger {
            override fun log(
                level: LogLevel,
                tag: String,
                message: String,
                throwable: Throwable?,
            ) {
                logged += listOf(level, tag, message, throwable)
            }
        }

    /** When each fetch ran, in virtual time since the test started. */
    private val fetchTimes = mutableListOf<Duration>()

    private fun TestScope.engine(
        random: Random = Random(SEED),
        maxJitter: Duration = Duration.ZERO,
    ) = DefaultPollingEngine(
        scope = backgroundScope,
        appLifecycle = appLifecycle,
        clock = { START + currentTime.milliseconds },
        timeSource = testScheduler.timeSource,
        logger = logger,
        jitter = randomJitter(random, maxJitter),
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
            val late = engine.poll(ARRIVALS, 20.seconds, recordingFetch())
            backgroundScope.launch { late.collect { second += it.value } }
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
            assertTrue(gaps.any { it < 20.seconds } && gaps.any { it > 20.seconds }, "one-sided jitter: $gaps")
        }

    @Test
    fun lb009_jitterNeverTakesAWaitBelowTheTenSecondFloor() =
        runTest {
            // LB-004 review: MIN_POLL_INTERVAL applies again after the jitter, so 10 s never becomes 9 s.
            // Both reviews of LB-009: the jitter stays spread at the floor, so clients there do not poll in step.
            val engine = engine(maxJitter = POLL_JITTER)
            val tooFast = mutableListOf<Duration>()

            backgroundScope.launch { engine.poll(VEHICLES, 10.seconds, recordingFetch()).collect {} }
            backgroundScope.launch {
                engine
                    .poll(PollKey("too-fast"), 5.seconds) {
                        tooFast += currentTime.milliseconds
                        AppResult.Success(0)
                    }.collect {}
            }
            advanceTimeBy(10.minutes)

            listOf(fetchTimes.gaps(), tooFast.gaps()).forEach { gaps ->
                assertTrue(gaps.all { it in 10.seconds..11.seconds }, "gaps: $gaps")
                assertTrue(gaps.count { it == 10.seconds } <= gaps.size / 10, "clamped at the floor: $gaps")
            }
        }

    @Test
    fun lb009_failedFetchesRetryAfterTwoFourEightSixteenAndThenThirtySeconds() =
        runTest {
            // FR-ERR-03. Fetch 1 succeeds, fetches 2 to 7 fail and fetch 8 succeeds. The retries are exact, without
            // jitter, and the last good value stays available with the error.
            val engine = engine(maxJitter = POLL_JITTER)
            val failing =
                recordingFetch { n ->
                    if (n in 2..7) AppResult.Failure(AppError.Network) else AppResult.Success(n)
                }
            val seen = mutableListOf<Timed<Int>>()

            backgroundScope.launch { engine.poll(ARRIVALS, 20.seconds, failing).collect { seen += it } }
            advanceTimeBy(3.minutes)

            val gaps = fetchTimes.gaps()
            assertEquals(listOf(2, 4, 8, 16, 30, 30).map { it.seconds }, gaps.subList(1, 7))
            assertTrue(gaps[0] in 19.seconds..21.seconds && gaps[7] in 19.seconds..21.seconds, "gaps: $gaps")
            assertEquals(
                listOf(
                    Timed(1, START, lastError = null),
                    Timed(1, START, lastError = AppError.Network),
                    Timed(8, START + fetchTimes[7], lastError = null),
                ),
                seen.take(3),
            )
        }

    @Test
    fun lb009_aFailureBeforeAnyValueCarriesOnlyTheError() =
        runTest {
            val failing = recordingFetch { AppResult.Failure(AppError.Server(503)) }

            engine().poll(ARRIVALS, 20.seconds, failing).test {
                assertEquals(Timed(null, fetchedAt = null, lastError = AppError.Server(503)), awaitItem())
            }
        }

    @Test
    fun lb009_theBackgroundStopsAllRequestsAndTheForegroundFetchesAtOnce() =
        runTest {
            // NFR-PERF-07 and spec §8.6: no polling in the background, and fresh data on return.
            val engine = engine()
            backgroundScope.launch { engine.poll(ARRIVALS, 20.seconds, recordingFetch()).collect {} }
            backgroundScope.launch { engine.poll(VEHICLES, 10.seconds, recordingFetch()).collect {} }
            advanceTimeBy(5.seconds)

            appLifecycle.isInForeground.value = false
            advanceTimeBy(5.minutes)
            assertEquals(listOf(0.seconds, 0.seconds), fetchTimes)

            appLifecycle.isInForeground.value = true
            runCurrent()
            assertEquals(listOf(0.seconds, 0.seconds, 305.seconds, 305.seconds), fetchTimes)

            // Polling then carries on at each key's interval.
            advanceTimeBy(21.seconds)
            assertEquals(listOf(0, 0, 305, 305, 315, 325, 325).map { it.seconds }, fetchTimes)
        }

    @Test
    fun lb009_aCollectorInTheBackgroundWaitsForTheForeground() =
        runTest {
            appLifecycle.isInForeground.value = false

            backgroundScope.launch { engine().poll(ARRIVALS, 20.seconds, recordingFetch()).collect {} }
            advanceTimeBy(1.minutes)
            assertEquals(emptyList(), fetchTimes)

            appLifecycle.isInForeground.value = true
            runCurrent()
            assertEquals(listOf(1.minutes), fetchTimes)
        }

    @Test
    fun lb009_theNewestCallerOfAKeySetsItsIntervalAndFetch() =
        runTest {
            // Remote config can change an interval during a session (spec §8.9): the next screen's interval applies,
            // from the next wait of the loop that is already running.
            val engine = engine()
            val seen = mutableListOf<Int?>()
            backgroundScope.launch { engine.poll(ARRIVALS, 20.seconds, recordingFetch()).collect {} }
            advanceTimeBy(30.seconds)

            val newer = engine.poll(ARRIVALS, 30.seconds, recordingFetch { AppResult.Success(100 + it) })
            backgroundScope.launch { newer.collect { seen += it.value } }
            advanceTimeBy(71.seconds)

            assertEquals(listOf(0, 20, 40, 70, 100).map { it.seconds }, fetchTimes)
            assertEquals(listOf<Int?>(2, 103, 104, 105), seen)
        }

    @Test
    fun lb009_aThrowingFetchFailsOnlyItsKeyAndIsRetried() =
        runTest {
            // NFR-REL-02: a bug in one resource's mapper neither stops the other keys nor crashes the app.
            val engine = engine()
            val seen = mutableListOf<Timed<Int>>()
            var vehicleFetches = 0
            val bug = IllegalStateException("Unexpected stop 42.4650,-2.4456")
            val throwing = recordingFetch { n -> if (n == 2) throw bug else AppResult.Success(n) }

            backgroundScope.launch { engine.poll(ARRIVALS, 20.seconds, throwing).collect { seen += it } }
            backgroundScope.launch {
                engine.poll(VEHICLES, 10.seconds) { AppResult.Success(++vehicleFetches) }.collect {}
            }
            advanceTimeBy(25.seconds)

            assertEquals(listOf(0.seconds, 20.seconds, 22.seconds), fetchTimes)
            assertEquals(Timed(1, START, lastError = AppError.Data), seen[1])
            assertEquals(Timed(3, START + 22.seconds, lastError = null), seen[2])
            assertEquals(3, vehicleFetches)
            // The key is an id, and the console logger prints the throwable's type only (NFR-PRIV-02).
            val entry = listOf<Any?>(LogLevel.Error, "PollingEngine", "Fetch threw for arrivals:152", bug)
            assertEquals(listOf(entry), logged)
        }

    @Test
    fun lb009_aTimeoutInsideFetchCountsAsANetworkFailure() =
        runTest {
            // A fetch that enforces spec §8.7's request timeout with withTimeout throws a CancellationException.
            val engine = engine()
            val seen = mutableListOf<Timed<Int>>()
            val timingOut: suspend () -> AppResult<Int> = {
                fetchTimes += currentTime.milliseconds
                if (fetchTimes.size == 2) withTimeout(10.seconds) { awaitCancellation() }
                AppResult.Success(fetchTimes.size)
            }

            backgroundScope.launch { engine.poll(ARRIVALS, 20.seconds, timingOut).collect { seen += it } }
            advanceTimeBy(35.seconds)

            assertEquals(listOf(0.seconds, 20.seconds, 32.seconds), fetchTimes)
            assertEquals(Timed(1, START, lastError = AppError.Network), seen[1])
        }

    @Test
    fun lb009_aFetchCancelledBecauseItsCollectorsLeftIsNotAnError() =
        runTest {
            // Guard for the two tests above: the engine's own cancellation still stops the fetch, silently.
            val engine = engine()
            val slow: suspend () -> AppResult<Int> = {
                fetchTimes += currentTime.milliseconds
                delay(5.seconds)
                AppResult.Success(fetchTimes.size)
            }
            val first = backgroundScope.launch { engine.poll(ARRIVALS, 20.seconds, slow).collect {} }
            advanceTimeBy(1.seconds)
            first.cancel()
            advanceTimeBy(1.seconds)

            engine.poll(ARRIVALS, 20.seconds, slow).test {
                assertEquals(Timed(2, START + 7.seconds, lastError = null), awaitItem())
            }
        }

    @Test
    fun lb009_aReturningCollectorWaitsForTheNextScheduledFetch() =
        runTest {
            // AGENTS.md §10: leaving and re-entering a screen never polls faster than the interval.
            val engine = engine()
            val seen = mutableListOf<Int?>()
            val first = backgroundScope.launch { engine.poll(ARRIVALS, 20.seconds, recordingFetch()).collect {} }
            advanceTimeBy(5.seconds)
            first.cancel()
            advanceTimeBy(5.seconds)

            backgroundScope.launch { engine.poll(ARRIVALS, 20.seconds, recordingFetch()).collect { seen += it.value } }
            advanceTimeBy(15.seconds)

            assertEquals(listOf(0.seconds, 20.seconds), fetchTimes)
            assertEquals(listOf<Int?>(1, 2), seen)
        }

    @Test
    fun lb009_theBackoffSurvivesResubscription() =
        runTest {
            // During an outage, re-entering the screen waits for the next retry instead of starting the ladder again.
            val engine = engine()
            val failing = recordingFetch { AppResult.Failure(AppError.Server(503)) }
            val first = backgroundScope.launch { engine.poll(ARRIVALS, 20.seconds, failing).collect {} }
            advanceTimeBy(7.seconds)
            first.cancel()
            advanceTimeBy(1.seconds)

            backgroundScope.launch { engine.poll(ARRIVALS, 20.seconds, failing).collect {} }
            advanceTimeBy(12.seconds)

            assertEquals(listOf(0.seconds, 2.seconds, 6.seconds, 14.seconds), fetchTimes)
        }

    @Test
    fun lb009_foregroundFlappingNeverPollsFasterThanTheFloor() =
        runTest {
            // Spec §8.6 asks for a fetch at once on return to the foreground, unless the last one is under 10 s old.
            val engine = engine()
            backgroundScope.launch { engine.poll(ARRIVALS, 20.seconds, recordingFetch()).collect {} }
            advanceTimeBy(1.seconds)
            appLifecycle.isInForeground.value = false
            advanceTimeBy(1.seconds)
            appLifecycle.isInForeground.value = true
            advanceTimeBy(29.seconds)
            appLifecycle.isInForeground.value = false
            advanceTimeBy(14.seconds)
            appLifecycle.isInForeground.value = true
            runCurrent()

            assertEquals(listOf(0.seconds, 10.seconds, 30.seconds, 45.seconds), fetchTimes)
        }

    @Test
    fun lb009_aFlappingSourceEscalatesTheBackoff() =
        runTest {
            // Data-rules review: an overloaded source that fails every other request is retried ever more slowly,
            // not after 2 s each time. A lone success waits the interval but does not reset the ladder.
            val engine = engine()
            val flapping =
                recordingFetch { n ->
                    if (n % 2 == 0) AppResult.Failure(AppError.Server(503)) else AppResult.Success(n)
                }

            backgroundScope.launch { engine.poll(VEHICLES, 10.seconds, flapping).collect {} }
            advanceTimeBy(3.minutes)

            val expected = listOf(10, 2, 10, 4, 10, 8, 10, 16, 10, 30, 10, 30).map { it.seconds }
            assertEquals(expected, fetchTimes.gaps().take(12))
        }

    @Test
    fun lb009_twoSuccessesInARowStartTheLadderAgain() =
        runTest {
            // Guard for the flapping rule: fetches 2 and 3 fail, 4 and 5 succeed, 6 and 7 fail.
            val engine = engine()
            val failing =
                recordingFetch { n ->
                    if (n in setOf(2, 3, 6, 7)) AppResult.Failure(AppError.Network) else AppResult.Success(n)
                }

            backgroundScope.launch { engine.poll(ARRIVALS, 20.seconds, failing).collect {} }
            advanceTimeBy(2.minutes)

            assertEquals(listOf(20, 2, 4, 20, 20, 2, 4).map { it.seconds }, fetchTimes.gaps().take(7))
        }

    @Test
    fun lb009_aValueLeftUncollectedForTwoMinutesIsNotReplayed() =
        runTest {
            // Code review: spec §5.7 calls a value over 120 s old expired. A screen that comes back after that long
            // shows Loading and then fresh data, not an expired value first.
            val engine = engine()
            val first = backgroundScope.launch { engine.poll(ARRIVALS, 20.seconds, recordingFetch()).collect {} }
            advanceTimeBy(5.seconds)
            first.cancel()
            advanceTimeBy(121.seconds)

            engine.poll(ARRIVALS, 20.seconds, recordingFetch()).test {
                assertEquals(Timed(2, START + 126.seconds, lastError = null), awaitItem())
            }
        }

    @Test
    fun lb009_anInfiniteIntervalIsRejectedWhenPolling() =
        runTest {
            // Data-rules review: delay(INFINITE) would park the key for good. Short ones are raised to the floor.
            assertFailsWith<IllegalArgumentException> {
                engine().poll(ARRIVALS, Duration.INFINITE, recordingFetch())
            }
        }

    @Test
    fun lb009_theProductionEngineWaitsTheIntervalPlusOrMinusOneSecond() =
        runTest {
            // The public constructor, as the composition root (LB-010) will call it.
            val clock = AppClock { START + currentTime.milliseconds }
            val engine = DefaultPollingEngine(backgroundScope, appLifecycle, clock, logger)

            backgroundScope.launch { engine.poll(ARRIVALS, 20.seconds, recordingFetch()).collect {} }
            advanceTimeBy(10.minutes)

            val gaps = fetchTimes.gaps()
            assertTrue(gaps.all { it in 19.seconds..21.seconds }, "gaps: $gaps")
            assertTrue(gaps.any { it < 20.seconds } && gaps.any { it > 20.seconds }, "one-sided jitter: $gaps")
        }

    private fun List<Duration>.gaps() = zipWithNext { previous, next -> next - previous }

    private companion object {
        const val SEED = 2026
        val START = Instant.parse("2026-10-26T08:00:00Z")
        val ARRIVALS = PollKey("arrivals:152")
        val VEHICLES = PollKey("vehicles")
    }
}
