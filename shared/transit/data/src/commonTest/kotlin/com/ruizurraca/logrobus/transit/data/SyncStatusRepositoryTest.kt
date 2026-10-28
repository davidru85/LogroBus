package com.ruizurraca.logrobus.transit.data

import app.cash.turbine.test
import com.ruizurraca.logrobus.core.common.AppDispatchers
import com.ruizurraca.logrobus.core.database.logroBusDatabase
import com.ruizurraca.logrobus.transit.domain.StaticDataset
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** FR-SET-03 and LB-404: the last successful sync of each dataset, as the static sync records it. */
class SyncStatusRepositoryTest {
    private val database = logroBusDatabase(testDriver())
    private val linesSynced = Instant.parse("2026-10-28T09:30:00Z")
    private val stopsSynced = Instant.parse("2026-10-27T22:15:00Z")

    private fun TestScope.repository() =
        SyncStatusRepositoryImpl(database, AppDispatchers(StandardTestDispatcher(testScheduler)))

    @Test
    fun lb404_datasetsAreStoredUnderKeysThatNeverChange() {
        assertEquals(listOf("lines", "stops"), StaticDataset.entries.map { it.key })
    }

    @Test
    fun lb404_noDatasetHasALastSuccessBeforeTheFirstSync() =
        runTest {
            repository().observeLastSuccesses().test {
                assertEquals(emptyMap(), awaitItem())
            }
        }

    @Test
    fun lb404_eachDatasetHasTheTimeOfItsLastSuccessfulSync() =
        runTest {
            database.syncQueries.recordSync(StaticDataset.LINES.key, linesSynced, "a")
            database.syncQueries.recordSync(StaticDataset.STOPS.key, stopsSynced, "b")

            repository().observeLastSuccesses().test {
                assertEquals(mapOf(StaticDataset.LINES to linesSynced, StaticDataset.STOPS to stopsSynced), awaitItem())
            }
        }

    @Test
    fun lb404_aNewSyncEmitsTheNewTime() =
        runTest {
            val earlier = Instant.parse("2026-10-27T09:30:00Z")
            database.syncQueries.recordSync(StaticDataset.LINES.key, earlier, "a")

            repository().observeLastSuccesses().test {
                assertEquals(mapOf(StaticDataset.LINES to earlier), awaitItem())
                database.syncQueries.recordSync(StaticDataset.LINES.key, linesSynced, "b")
                assertEquals(mapOf(StaticDataset.LINES to linesSynced), awaitItem())
            }
        }

    @Test
    fun lb404_rowsWithoutASuccessOrOfOtherDatasetsAreIgnored() =
        runTest {
            database.syncQueries.recordSync(StaticDataset.STOPS.key, null, "b")
            database.syncQueries.recordSync("news", linesSynced, "c")

            repository().observeLastSuccesses().test {
                assertEquals(emptyMap(), awaitItem())
            }
        }
}
