package com.ruizurraca.logrobus.transit.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.ruizurraca.logrobus.core.common.AppDispatchers
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.database.LogroBusDatabase
import com.ruizurraca.logrobus.transit.domain.StaticDataset
import com.ruizurraca.logrobus.transit.domain.SyncStatusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

/** Reads sync_state, which the static sync writes (architecture.md §4.4). */
class SyncStatusRepositoryImpl(
    private val database: LogroBusDatabase,
    private val dispatchers: AppDispatchers,
    private val logger: Logger,
) : SyncStatusRepository {
    override fun observeLastSuccesses(): Flow<AppResult<Map<StaticDataset, Instant>>> =
        database.syncQueries
            .lastSuccesses()
            .asFlow()
            .mapToList(dispatchers.io)
            .map { rows -> rows.mapNotNull { row -> datasetOf(row.dataset)?.let { it to row.last_success } }.toMap() }
            .map { AppResult.Success(it) }

    // v1.0 datasets may already have rows; the MVP has no use for them.
    private fun datasetOf(key: String): StaticDataset? = StaticDataset.entries.firstOrNull { it.key == key }
}

/** The dataset's key in sync_state. Stored on devices, so it never changes with the enum's names. */
internal val StaticDataset.key: String
    get() =
        when (this) {
            StaticDataset.LINES -> "lines"
            StaticDataset.STOPS -> "stops"
        }
