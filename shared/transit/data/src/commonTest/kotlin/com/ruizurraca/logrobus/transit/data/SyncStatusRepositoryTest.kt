package com.ruizurraca.logrobus.transit.data

import app.cash.turbine.test
import com.ruizurraca.logrobus.core.common.AppDispatchers
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.LogLevel
import com.ruizurraca.logrobus.core.database.logroBusDatabase
import com.ruizurraca.logrobus.transit.domain.StaticDataset
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Instant

/** FR-SET-03 and LB-404: the last successful sync of each dataset, as the static sync records it. */
class SyncStatusRepositoryTest {
    private val driver = testDriver()
    private val database = logroBusDatabase(driver)
    private val logger = RecordingLogger()
    private val linesSynced = Instant.parse("2026-10-28T09:30:00Z")
    private val stopsSynced = Instant.parse("2026-10-27T22:15:00Z")

    private fun TestScope.repository() =
        SyncStatusRepositoryImpl(database, AppDispatchers(StandardTestDispatcher(testScheduler)), logger)

    @AfterTest
    fun closeDatabase() = driver.close()

    @Test
    fun lb404_datasetsAreStoredUnderKeysThatNeverChange() {
        assertEquals(listOf("lines", "stops"), StaticDataset.entries.map { it.key })
    }

    @Test
    fun lb404_noDatasetHasALastSuccessBeforeTheFirstSync() =
        runTest {
            repository().observeLastSuccesses().test {
                assertEquals(AppResult.Success(emptyMap()), awaitItem())
            }
        }

    @Test
    fun lb404_eachDatasetHasTheTimeOfItsLastSuccessfulSync() =
        runTest {
            database.syncQueries.recordSync(StaticDataset.LINES.key, linesSynced, "a")
            database.syncQueries.recordSync(StaticDataset.STOPS.key, stopsSynced, "b")

            repository().observeLastSuccesses().test {
                assertEquals(
                    AppResult.Success(mapOf(StaticDataset.LINES to linesSynced, StaticDataset.STOPS to stopsSynced)),
                    awaitItem(),
                )
            }
        }

    @Test
    fun lb404_aNewSyncEmitsTheNewTime() =
        runTest {
            val earlier = Instant.parse("2026-10-27T09:30:00Z")
            database.syncQueries.recordSync(StaticDataset.LINES.key, earlier, "a")

            repository().observeLastSuccesses().test {
                assertEquals(AppResult.Success(mapOf(StaticDataset.LINES to earlier)), awaitItem())
                database.syncQueries.recordSync(StaticDataset.LINES.key, linesSynced, "b")
                assertEquals(AppResult.Success(mapOf(StaticDataset.LINES to linesSynced)), awaitItem())
            }
        }

    @Test
    fun lb404_rowsWithoutASuccessOrOfOtherDatasetsAreIgnored() =
        runTest {
            database.syncQueries.recordSync(StaticDataset.STOPS.key, null, "b")
            database.syncQueries.recordSync("news", linesSynced, "c")

            repository().observeLastSuccesses().test {
                assertEquals(AppResult.Success(emptyMap()), awaitItem())
            }
        }

    @Test
    fun lb010_aDatabaseThatCannotBeReadGivesAFailure_notACrash() =
        runTest {
            driver.execute(null, "DROP TABLE sync_state", 0)

            repository().observeLastSuccesses().test {
                assertEquals(AppResult.Failure(AppError.Data), awaitItem())
                awaitComplete()
            }
            // Logged by type: ConsoleLogger prints the throwable's types, never its message (NFR-PRIV-02).
            val entry = logger.entries.single()
            assertEquals(LogLevel.Warn, entry.level)
            assertNotNull(entry.throwable)
        }
}
