package com.ruizurraca.logrobus.feature.lines.domain

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.transit.domain.Line
import com.ruizurraca.logrobus.transit.domain.LinesRepository
import com.ruizurraca.logrobus.transit.domain.StaticSyncEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

/**
 * FR-LINE-01: every line, or the failure to get them. An interface, because Swift calls it and Swift tests replace it
 * (architecture.md §4.6).
 */
fun interface ObserveLines {
    operator fun invoke(): Flow<AppResult<List<Line>>>
}

/**
 * The cached lines, with a sync on every collection (ADR-0008): the first use fills the empty cache, and a later one
 * refreshes a stale cache. While the cache is empty the lines wait for the sync, whose failure is then the result
 * (FR-ERR-05); once there are lines, a failed refresh is silent (FR-ERR-04). Retry collects it again.
 */
class DefaultObserveLines(
    private val sync: StaticSyncEngine,
    private val repository: LinesRepository,
) : ObserveLines {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(): Flow<AppResult<List<Line>>> =
        channelFlow {
            // Null while the sync runs.
            val synced = MutableStateFlow<AppResult<Unit>?>(null)
            launch { synced.value = sync.syncIfStale() }
            // A new read once the sync ends, so that its lines are read after its transaction, never before.
            synced
                .flatMapLatest { outcome -> repository.observeLines().mapNotNull { resultOf(it, outcome) } }
                .distinctUntilChanged()
                .collect { send(it) }
        }

    private fun resultOf(
        lines: AppResult<List<Line>>,
        sync: AppResult<Unit>?,
    ): AppResult<List<Line>>? =
        when (lines) {
            is AppResult.Failure -> lines
            is AppResult.Success -> if (lines.value.isNotEmpty()) lines else resultOfEmptyCache(lines, sync)
        }

    // An empty cache waits for the sync, whose failure is then the result (FR-ERR-05).
    private fun resultOfEmptyCache(
        lines: AppResult.Success<List<Line>>,
        sync: AppResult<Unit>?,
    ): AppResult<List<Line>>? =
        when (sync) {
            null -> null
            is AppResult.Failure -> sync
            is AppResult.Success -> lines
        }
}
