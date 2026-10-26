package com.ruizurraca.logrobus.core.polling

import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.common.AppLifecycle
import com.ruizurraca.logrobus.core.common.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random
import kotlin.time.Duration

class DefaultPollingEngine internal constructor(
    private val scope: CoroutineScope,
    private val appLifecycle: AppLifecycle,
    private val clock: AppClock,
    private val random: Random,
    private val maxJitter: Duration,
) : PollingEngine {
    private val mutex = Mutex()
    private val polls = mutableMapOf<PollKey, Poll<*>>()

    override fun <T> poll(
        key: PollKey,
        interval: Duration,
        fetch: suspend () -> AppResult<T>,
    ): Flow<Timed<T>> =
        flow {
            @Suppress("UNCHECKED_CAST") // A key identifies one request, so its values always have the same type.
            val poll = mutex.withLock { polls.getOrPut(key) { Poll(interval, fetch) } } as Poll<T>
            emitAll(poll.latest.filterNotNull())
        }

    /** One shared loop per key, which runs while [latest] has collectors. */
    private inner class Poll<T>(
        private val interval: Duration,
        private val fetch: suspend () -> AppResult<T>,
    ) {
        val latest = MutableStateFlow<Timed<T>?>(null)

        init {
            scope.launch {
                latest.subscriptionCount
                    .map { it > 0 }
                    .distinctUntilChanged()
                    .collectLatest { collected -> if (collected) run() }
            }
        }

        private suspend fun run() {
            while (true) {
                val result = fetch()
                if (result is AppResult.Success) latest.value = Timed(result.value, clock.now(), lastError = null)
                delay(interval)
            }
        }
    }
}
