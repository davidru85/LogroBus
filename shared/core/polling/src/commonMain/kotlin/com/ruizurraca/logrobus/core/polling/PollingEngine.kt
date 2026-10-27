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
     * [Timed] value. The flow never completes; stop collecting to stop polling.
     *
     * - **Sharing:** collectors of the same [key] share one request per [interval]. A new collector gets the latest
     *   value at once, unless the key has had no collector for 2 min (spec §5.7: that value is expired). Each new
     *   collection sets the key's [interval] and [fetch], from the key's next wait.
     * - **Schedule:** each wait is [interval], at least `MIN_POLL_INTERVAL`, with ±1 s of jitter. A key keeps its
     *   schedule when collectors leave and come back. Returning to the foreground fetches at once, unless the last
     *   attempt is under `MIN_POLL_INTERVAL` old (spec §8.6).
     * - **Failures (FR-ERR-03):** the next attempts wait 2, 4, 8 and 16 s, then 30 s, and the last good value stays
     *   with [Timed.lastError] set. A lone success between failures waits the interval without restarting that
     *   ladder; two in a row do.
     *
     * [fetch] reports failures as [AppResult.Failure]. If it throws all the same, the key gets a [AppError.Data]
     * failure (a [kotlinx.coroutines.CancellationException], such as a timeout, gives [AppError.Network]), and
     * the other keys are not affected. [fetch] should capture only data-layer objects: the engine keeps the newest
     * one for the key.
     */
    fun <T> poll(
        key: PollKey,
        interval: Duration,
        fetch: suspend () -> AppResult<T>,
    ): Flow<Timed<T>>
}

/**
 * Identifies one live request and the type of its value, as `<resource>:<id>`, for example `arrivals:152`. Keys hold
 * ids only, never coordinates or search text: the engine logs them (NFR-PRIV-02).
 */
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
