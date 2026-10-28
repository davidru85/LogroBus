package com.ruizurraca.logrobus.feature.settings.domain

import app.cash.turbine.test
import com.ruizurraca.logrobus.transit.domain.StaticDataset
import com.ruizurraca.logrobus.transit.domain.SyncStatusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** FR-SET-03: About data lists every static dataset, synced or not. */
class ObserveLastSyncsTest {
    private val linesSynced = Instant.parse("2026-10-28T09:30:00Z")
    private val stopsSynced = Instant.parse("2026-10-28T09:31:00Z")
    private val lastSuccesses = MutableStateFlow(mapOf(StaticDataset.LINES to linesSynced))
    private val observeLastSyncs =
        DefaultObserveLastSyncs(
            object : SyncStatusRepository {
                override fun observeLastSuccesses() = lastSuccesses
            },
        )

    @Test
    fun lb404_everyDatasetIsListedInOrder_withItsLastSuccessOrNone() =
        runTest {
            observeLastSyncs().test {
                assertEquals(
                    listOf(DatasetSync(StaticDataset.LINES, linesSynced), DatasetSync(StaticDataset.STOPS, null)),
                    awaitItem(),
                )
            }
        }

    @Test
    fun lb404_aNewSyncUpdatesTheList() =
        runTest {
            observeLastSyncs().test {
                awaitItem()
                lastSuccesses.value = mapOf(StaticDataset.LINES to linesSynced, StaticDataset.STOPS to stopsSynced)
                assertEquals(
                    listOf(
                        DatasetSync(StaticDataset.LINES, linesSynced),
                        DatasetSync(StaticDataset.STOPS, stopsSynced),
                    ),
                    awaitItem(),
                )
            }
        }
}
