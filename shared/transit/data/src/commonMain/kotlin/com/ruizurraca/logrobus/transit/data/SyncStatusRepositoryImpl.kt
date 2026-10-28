package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.common.AppDispatchers
import com.ruizurraca.logrobus.core.database.LogroBusDatabase
import com.ruizurraca.logrobus.transit.domain.StaticDataset
import com.ruizurraca.logrobus.transit.domain.SyncStatusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.time.Instant

/** Reads sync_state, which the static sync writes (architecture.md §4.4). */
class SyncStatusRepositoryImpl(
    private val database: LogroBusDatabase,
    private val dispatchers: AppDispatchers,
) : SyncStatusRepository {
    override fun observeLastSuccesses(): Flow<Map<StaticDataset, Instant>> = emptyFlow()
}

/** The dataset's key in sync_state. Stored on devices, so it never changes with the enum's names. */
internal val StaticDataset.key: String
    get() = ""
