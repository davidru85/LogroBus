package com.ruizurraca.logrobus.core.polling

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import kotlinx.coroutines.flow.Flow
import kotlin.jvm.JvmInline
import kotlin.time.Duration
import kotlin.time.Instant

/** The only way live data is polled (spec §8.6, AGENTS.md §5.7). */
interface PollingEngine {
    /**
     * Polls [fetch] while the returned flow is collected and the app is in the foreground, and emits the latest
     * [Timed] value. Collectors of the same [key] share one request per [interval], and a new collector gets the
     * latest value at once. The newest caller of a key sets its [interval] and [fetch].
     *
     * Each wait is [interval] with ±1 s of jitter, never below `MIN_POLL_INTERVAL`. After a failure, the next
     * attempts wait 2, 4, 8 and 16 s, then 30 s (FR-ERR-03). Returning to the foreground fetches at once.
     *
     * [fetch] reports failures as [AppResult.Failure] and never throws. The flow never completes; stop collecting
     * to stop polling.
     */
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
