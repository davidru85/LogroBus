package com.ruizurraca.logrobus.feature.settings.domain

import com.ruizurraca.logrobus.transit.domain.StaticDataset
import com.ruizurraca.logrobus.transit.domain.SyncStatusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

/** When [dataset] last synced successfully, or null if it never has. */
data class DatasetSync(
    val dataset: StaticDataset,
    val lastSuccess: Instant?,
)

/**
 * FR-SET-03: the last successful sync of every static dataset, for About data (LB-404). An interface, because Swift
 * calls it and Swift tests replace it (architecture.md §4.6).
 */
fun interface ObserveLastSyncs {
    operator fun invoke(): Flow<List<DatasetSync>>
}

class DefaultObserveLastSyncs(
    private val repository: SyncStatusRepository,
) : ObserveLastSyncs {
    override fun invoke(): Flow<List<DatasetSync>> =
        repository.observeLastSuccesses().map { last -> StaticDataset.entries.map { DatasetSync(it, last[it]) } }
}
