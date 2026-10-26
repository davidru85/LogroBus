package com.ruizurraca.logrobus.core.polling

import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.common.AppLifecycle
import com.ruizurraca.logrobus.core.common.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.random.Random
import kotlin.time.Duration

class DefaultPollingEngine internal constructor(
    private val scope: CoroutineScope,
    private val appLifecycle: AppLifecycle,
    private val clock: AppClock,
    private val random: Random,
    private val maxJitter: Duration,
) : PollingEngine {
    override fun <T> poll(
        key: PollKey,
        interval: Duration,
        fetch: suspend () -> AppResult<T>,
    ): Flow<Timed<T>> = emptyFlow()
}
