package com.ruizurraca.logrobus.core.polling

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import kotlinx.coroutines.flow.Flow
import kotlin.jvm.JvmInline
import kotlin.time.Duration
import kotlin.time.Instant

/** The only way live data is polled (spec §8.6, AGENTS.md §5.7). */
interface PollingEngine {
    fun <T> poll(
        key: PollKey,
        interval: Duration,
        fetch: suspend () -> AppResult<T>,
    ): Flow<Timed<T>>
}

/** Identifies one live request, such as the arrivals at one stop. */
@JvmInline
value class PollKey(
    val value: String,
)

/** The latest [value] of a polled request, when it was fetched, and the error of the latest attempt, if it failed. */
data class Timed<out T>(
    val value: T?,
    val fetchedAt: Instant?,
    val lastError: AppError?,
)
