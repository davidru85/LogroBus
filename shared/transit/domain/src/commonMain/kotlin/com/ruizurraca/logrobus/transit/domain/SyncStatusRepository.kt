package com.ruizurraca.logrobus.transit.domain

import com.ruizurraca.logrobus.core.common.AppResult
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/** What the static sync has achieved so far (spec §8.5, FR-SET-03). */
interface SyncStatusRepository {
    /**
     * The last successful sync of each dataset that has one, emitted again whenever sync_state changes. A failure to
     * read is a [AppResult.Failure], never an exception (AGENTS.md §5, rule 11).
     */
    fun observeLastSuccesses(): Flow<AppResult<Map<StaticDataset, Instant>>>
}
