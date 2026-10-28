package com.ruizurraca.logrobus.transit.domain

import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/** What the static sync has achieved so far (spec §8.5, FR-SET-03). */
interface SyncStatusRepository {
    /** The last successful sync of each dataset that has one; emits again after every sync. */
    fun observeLastSuccesses(): Flow<Map<StaticDataset, Instant>>
}
