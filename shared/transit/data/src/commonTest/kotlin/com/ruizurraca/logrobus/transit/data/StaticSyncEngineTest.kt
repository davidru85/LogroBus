package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.common.AppDispatchers
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.database.logroBusDatabase
import com.ruizurraca.logrobus.core.database.searchStops
import com.ruizurraca.logrobus.transit.domain.StaticDataset
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** Spec §8.5, architecture.md §4.4, ADR-0008: the static sync of lines and stops. */
class StaticSyncEngineTest {
    private val driver = testDriver()
    private val database = logroBusDatabase(driver)
    private val logger = RecordingLogger()
    private val source = FakeStaticSource()
    private val normaliser = FakeNormaliser()
    private val now = Instant.parse("2026-10-30T08:00:00Z")

    private fun TestScope.engine() =
        DefaultStaticSyncEngine(
            source = source,
            normaliser = normaliser,
            database = database,
            clock = AppClock { now },
            dispatchers = AppDispatchers(StandardTestDispatcher(testScheduler)),
            logger = logger,
        )

    @AfterTest
    fun closeDatabase() = driver.close()

    @Test
    fun lb008_theFirstSyncStoresTheLinesPatternsShapesAndStopsItDownloads() =
        runTest {
            assertEquals(AppResult.Success(Unit), engine().syncIfStale())

            assertEquals(catalogue(), driver.storedCatalogue())
        }

    @Test
    fun lb008_storedStopsCanBeSearchedByName() =
        runTest {
            engine().syncIfStale()

            assertEquals(listOf(42L), database.searchStops("colon 42 v1").map { it.id })
        }

    @Test
    fun lb008_aSuccessfulSyncRecordsWhenEachDatasetSyncedAndTheHashOfItsBody() =
        runTest {
            engine().syncIfStale()

            val lines = database.syncQueries.syncState(StaticDataset.LINES.key).executeAsOne()
            val stops = database.syncQueries.syncState(StaticDataset.STOPS.key).executeAsOne()
            assertEquals(now to sha256Hex(body("lines", 1).toString()), lines.last_success to lines.content_hash)
            assertEquals(now to sha256Hex(body("stops", 1).toString()), stops.last_success to stops.content_hash)
        }
}
