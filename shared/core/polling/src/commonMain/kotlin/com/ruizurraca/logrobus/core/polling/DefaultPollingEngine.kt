package com.ruizurraca.logrobus.core.polling

import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.common.AppLifecycle
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.config.MIN_POLL_INTERVAL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Spec §8.6: each wait is the interval plus or minus up to this much. */
internal val POLL_JITTER = 1.seconds

private val FIRST_RETRY = 2.seconds
private val MAX_RETRY = 30.seconds

/** Enough doublings to pass [MAX_RETRY], few enough that the shift never overflows. */
private const val MAX_DOUBLINGS = 4

/**
 * The app's one [PollingEngine]. Each key's loop runs in [scope], an application-wide scope, while the key has
 * collectors and [appLifecycle] says the app is in the foreground. Values are stamped with [clock], the
 * server-corrected time that freshness is measured against.
 */
class DefaultPollingEngine internal constructor(
    private val scope: CoroutineScope,
    private val appLifecycle: AppLifecycle,
    private val clock: AppClock,
    private val random: Random,
    private val maxJitter: Duration,
) : PollingEngine {
    constructor(
        scope: CoroutineScope,
        appLifecycle: AppLifecycle,
        clock: AppClock,
    ) : this(scope, appLifecycle, clock, Random.Default, POLL_JITTER)

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
                    (polls.getOrPut(key) { Poll(interval, fetch) } as Poll<T>).also {
                        it.interval = interval
                        it.fetch = fetch
                    }
                }
            emitAll(poll.latest.filterNotNull())
        }

    /** One shared loop per key, which runs while [latest] has collectors and the app is in the foreground. */
    private inner class Poll<T>(
        @Volatile var interval: Duration,
        @Volatile var fetch: suspend () -> AppResult<T>,
    ) {
        val latest = MutableStateFlow<Timed<T>?>(null)

        init {
            scope.launch {
                combine(latest.subscriptionCount, appLifecycle.isInForeground) { collectors, inForeground ->
                    collectors > 0 && inForeground
                }.distinctUntilChanged().collectLatest { active -> if (active) loop() }
            }
        }

        private suspend fun loop() {
            var failures = 0
            while (true) {
                when (val result = fetch()) {
                    is AppResult.Success -> {
                        failures = 0
                        latest.value = Timed(result.value, clock.now(), lastError = null)
                    }

                    is AppResult.Failure -> {
                        failures++
                        latest.value = latest.value?.copy(lastError = result.error)
                            ?: Timed(value = null, fetchedAt = null, lastError = result.error)
                    }
                }
                delay(if (failures == 0) nextPoll() else backoff(failures))
            }
        }

        /** The interval with jitter, raised to the floor afterwards, so that 10 s never becomes 9 s. */
        private fun nextPoll(): Duration = (interval + jitter()).coerceAtLeast(MIN_POLL_INTERVAL)
    }

    /** FR-ERR-03: 2, 4, 8 and 16 s, then 30 s. */
    private fun backoff(failures: Int): Duration =
        (FIRST_RETRY * (1 shl (failures - 1).coerceAtMost(MAX_DOUBLINGS))).coerceAtMost(MAX_RETRY)

    private fun jitter(): Duration {
        val maxMs = maxJitter.inWholeMilliseconds
        return random.nextLong(-maxMs, maxMs + 1).milliseconds
    }
}
