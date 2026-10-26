package com.ruizurraca.logrobus.core.polling

import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppLifecycle
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.error
import com.ruizurraca.logrobus.core.config.MIN_POLL_INTERVAL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile
import kotlin.random.Random
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/** Spec §8.6: each wait is the interval plus or minus up to this much. */
internal val POLL_JITTER = 1.seconds

private const val TAG = "PollingEngine"

private val FIRST_RETRY = 2.seconds
private val MAX_RETRY = 30.seconds

/** A lone success between failures does not reset the backoff, so a flapping source is retried ever more slowly. */
private const val SUCCESSES_TO_RESET = 2

/** Enough doublings to pass [MAX_RETRY], few enough that the shift never overflows. */
private const val MAX_DOUBLINGS = 4

/**
 * The app's one [PollingEngine]. Each key's loop runs in [scope], an application-wide scope, while the key has
 * collectors and [appLifecycle] says the app is in the foreground. Values are stamped with [clock], the
 * server-corrected time that freshness is measured against.
 */
class DefaultPollingEngine internal constructor(
    scope: CoroutineScope,
    private val appLifecycle: AppLifecycle,
    private val clock: AppClock,
    private val timeSource: TimeSource.WithComparableMarks,
    private val logger: Logger,
    private val jitter: () -> Duration,
) : PollingEngine {
    constructor(
        scope: CoroutineScope,
        appLifecycle: AppLifecycle,
        clock: AppClock,
        logger: Logger,
    ) : this(scope, appLifecycle, clock, TimeSource.Monotonic, logger, randomJitter(Random.Default, POLL_JITTER))

    // A child supervisor: a failure in one key's coroutine never cancels the others, nor the caller's scope.
    private val keysScope = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]))
    private val mutex = Mutex()
    private val polls = mutableMapOf<PollKey, Poll<*>>()

    override fun <T> poll(
        key: PollKey,
        interval: Duration,
        fetch: suspend () -> AppResult<T>,
    ): Flow<Timed<T>> =
        flow {
            val poll =
                mutex.withLock {
                    // A key identifies one request, so its values always have the same type.
                    @Suppress("UNCHECKED_CAST")
                    (polls.getOrPut(key) { Poll(key, interval, fetch) } as Poll<T>).also {
                        it.interval = interval
                        it.fetch = fetch
                    }
                }
            emitAll(poll.latest.filterNotNull())
        }

    /** One shared loop per key, which runs while [latest] has collectors and the app is in the foreground. */
    private inner class Poll<T>(
        private val key: PollKey,
        @Volatile var interval: Duration,
        @Volatile var fetch: suspend () -> AppResult<T>,
    ) {
        val latest = MutableStateFlow<Timed<T>?>(null)

        // The schedule outlives each activation. Only this key's coroutine touches it, one activation at a time.
        private var failures = 0
        private var successesInARow = 0
        private var lastAttempt: ComparableTimeMark? = null
        private var plannedWait = Duration.ZERO
        private var wasInBackground = false

        init {
            keysScope.launch {
                combine(latest.subscriptionCount, appLifecycle.isInForeground) { collectors, inForeground ->
                    (collectors > 0) to inForeground
                }.distinctUntilChanged().collectLatest { (collected, inForeground) ->
                    if (!inForeground) wasInBackground = true
                    if (collected && inForeground) {
                        val resumed = wasInBackground
                        wasInBackground = false
                        delay(firstWait(resumed))
                        loop()
                    }
                }
            }
        }

        /**
         * What is left of the wait planned after the last attempt. On return to the foreground, spec §8.6 asks for a
         * fetch at once, so the wait shrinks to the floor: a value fetched under 10 s ago is still fresh.
         */
        private fun firstWait(resumed: Boolean): Duration {
            val last = lastAttempt ?: return Duration.ZERO
            val wait = if (resumed) minOf(plannedWait, MIN_POLL_INTERVAL) else plannedWait
            return wait - last.elapsedNow()
        }

        private suspend fun loop() {
            while (true) {
                plannedWait =
                    when (val result = attempt()) {
                        is AppResult.Success -> {
                            if (++successesInARow >= SUCCESSES_TO_RESET) failures = 0
                            latest.value = Timed(result.value, clock.now(), lastError = null)
                            nextPoll()
                        }

                        is AppResult.Failure -> {
                            successesInARow = 0
                            failures++
                            latest.value = latest.value?.copy(lastError = result.error)
                                ?: Timed(value = null, fetchedAt = null, lastError = result.error)
                            backoff(failures)
                        }
                    }
                lastAttempt = timeSource.markNow()
                delay(plannedWait)
            }
        }

        /** Calls [fetch], turning anything it throws into a failure of this key alone (NFR-REL-02). */
        @Suppress("TooGenericExceptionCaught", "SwallowedException") // On purpose: see above.
        private suspend fun attempt(): AppResult<T> =
            try {
                fetch()
            } catch (e: CancellationException) {
                // The engine's own cancellation propagates; a timeout inside fetch is a network failure.
                currentCoroutineContext().ensureActive()
                AppResult.Failure(AppError.Network)
            } catch (e: Exception) {
                logger.error(TAG, "Fetch threw for ${key.value}", e)
                AppResult.Failure(AppError.Data)
            }

        /**
         * The interval, at least the floor, with jitter. A wait that the jitter takes below the floor is reflected
         * above it, so that 10 s never becomes 9 s and the waits at the floor still spread over 10 to 11 s.
         */
        private fun nextPoll(): Duration {
            val wait = interval.coerceAtLeast(MIN_POLL_INTERVAL) + jitter()
            return if (wait < MIN_POLL_INTERVAL) MIN_POLL_INTERVAL + (MIN_POLL_INTERVAL - wait) else wait
        }
    }

    /** FR-ERR-03: 2, 4, 8 and 16 s, then 30 s. */
    private fun backoff(failures: Int): Duration =
        (FIRST_RETRY * (1 shl (failures - 1).coerceAtMost(MAX_DOUBLINGS))).coerceAtMost(MAX_RETRY)
}

/** A uniform jitter from -[max] to +[max], in whole milliseconds. */
internal fun randomJitter(
    random: Random,
    max: Duration,
): () -> Duration {
    val maxMs = max.inWholeMilliseconds
    return { random.nextLong(-maxMs, maxMs + 1).milliseconds }
}
