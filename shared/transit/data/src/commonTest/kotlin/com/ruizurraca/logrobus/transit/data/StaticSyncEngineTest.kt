package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.common.AppDispatchers
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.LogLevel
import com.ruizurraca.logrobus.core.database.logroBusDatabase
import com.ruizurraca.logrobus.core.database.searchStops
import com.ruizurraca.logrobus.transit.domain.StaticDataset
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Spec §8.5, architecture.md §4.4, ADR-0008: the static sync of lines and stops. */
class StaticSyncEngineTest {
    private val driver = testDriver()
    private val database = logroBusDatabase(driver)
    private val logger = RecordingLogger()
    private val source = FakeStaticSource()
    private val normaliser = FakeNormaliser()
    private var now = Instant.parse("2026-10-30T08:00:00Z")

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

    @Test
    fun lb008_withNoNetworkTheFirstSyncFailsWithNetworkAndStoresNothing() =
        runTest {
            source.lines = AppResult.Failure(AppError.Network)

            assertEquals(AppResult.Failure(AppError.Network), engine().syncIfStale())

            assertEquals(nothing, driver.storedCatalogue())
            assertEquals(emptyList(), database.syncQueries.lastSuccesses().executeAsList())
        }

    @Test
    fun lb008_aRetrySucceedsOnceTheNetworkIsBack() =
        runTest {
            val engine = engine()
            source.stops = AppResult.Failure(AppError.Network)
            engine.syncIfStale()

            source.stops = AppResult.Success(body("stops", 1))

            assertEquals(AppResult.Success(Unit), engine.syncIfStale())
            assertEquals(catalogue(), driver.storedCatalogue())
        }

    @Test
    fun lb008_aPayloadTheNormaliserCannotReadStoresNothing() =
        runTest {
            normaliser.result = AppResult.Failure(AppError.Data)

            assertEquals(AppResult.Failure(AppError.Data), engine().syncIfStale())

            assertEquals(nothing, driver.storedCatalogue())
        }

    @Test
    fun lb008_aPayloadWithFewerThanFiveLinesIsRejectedAndTheCacheKept() =
        runTest {
            assertRejectedAndCacheKept(catalogue(version = 2, lineCount = FEWEST_LINES - 1))
        }

    @Test
    fun lb008_aPayloadWithFewerThanOneHundredStopsIsRejectedAndTheCacheKept() =
        runTest {
            assertRejectedAndCacheKept(catalogue(version = 2, stopCount = FEWEST_STOPS - 1))
        }

    @Test
    fun lb008_provisionalStopsDoNotCountTowardsTheHundred() =
        runTest {
            // DD-10: a provisional stop is made up from a pattern, not delivered by API-02.
            val payload = catalogue(version = 2, stopCount = FEWEST_STOPS + 2)
            val provisional = payload.stops.take(3).map { it.copy(isProvisional = true) }

            assertRejectedAndCacheKept(payload.copy(stops = provisional + payload.stops.drop(3)))
        }

    @Test
    fun lb008_aRejectedPayloadIsLoggedWithItsCounts() =
        runTest {
            normaliser.result = AppResult.Success(catalogue(lineCount = 4, stopCount = 98))

            engine().syncIfStale()

            assertTrue(logger.entries.any { it.level == LogLevel.Warn && "4 lines and 98 stops" in it.message })
        }

    @Test
    fun lb008_withinTwentyFourHoursOfTheLastSuccessNothingIsDownloaded() =
        runTest {
            val engine = engine()
            engine.syncIfStale()
            now += 24.hours - 1.minutes

            assertEquals(AppResult.Success(Unit), engine.syncIfStale())

            assertEquals(2, source.requests)
        }

    @Test
    fun lb008_twentyFourHoursAfterTheLastSuccessTheSyncDownloadsAgain() =
        runTest {
            val engine = engine()
            engine.syncIfStale()
            now += 24.hours
            publish(version = 2)

            assertEquals(AppResult.Success(Unit), engine.syncIfStale())

            assertEquals(catalogue(version = 2), driver.storedCatalogue())
        }

    @Test
    fun lb008_aDatasetThatNeverSyncedMakesTheCacheStale() =
        runTest {
            database.syncQueries.recordSync(StaticDataset.LINES.key, now, "a hash")

            engine().syncIfStale()

            assertEquals(2, source.requests)
        }

    @Test
    fun lb008_aFailedRefreshIsRetriedAtTheNextTrigger() =
        runTest {
            val engine = engine()
            engine.syncIfStale()
            now += 25.hours
            source.lines = AppResult.Failure(AppError.Server(503))
            engine.syncIfStale()

            now += 1.minutes
            publish(version = 2)

            assertEquals(AppResult.Success(Unit), engine.syncIfStale())
            assertEquals(catalogue(version = 2), driver.storedCatalogue())
        }

    @Test
    fun lb008_anUnchangedPayloadWritesNothingButTheTimeOfTheSync() =
        runTest {
            val engine = engine()
            engine.syncIfStale()
            val firstSync = database.syncQueries.syncState(StaticDataset.LINES.key).executeAsOne()
            driver.execute(null, "UPDATE line SET display_name = 'edited' WHERE id = 1", 0)
            now += 25.hours
            normaliser.result = AppResult.Success(catalogue(version = 2))

            assertEquals(AppResult.Success(Unit), engine.syncIfStale())

            // The row edited above is still edited: nothing was replaced, and nothing was normalised.
            assertEquals(
                listOf("edited"),
                driver.select("SELECT display_name FROM line WHERE id = 1") { it.getString(0) },
            )
            assertEquals(1, normaliser.calls)
            assertEquals(
                firstSync.copy(last_success = now),
                database.syncQueries.syncState(StaticDataset.LINES.key).executeAsOne(),
            )
            assertEquals(
                now,
                database.syncQueries
                    .syncState(StaticDataset.STOPS.key)
                    .executeAsOne()
                    .last_success,
            )
        }

    @Test
    fun lb008_whenOneDatasetChangesBothAreStored() =
        runTest {
            val engine = engine()
            engine.syncIfStale()
            now += 25.hours
            source.stops = AppResult.Success(body("stops", 2))
            normaliser.result = AppResult.Success(catalogue(version = 2))

            engine.syncIfStale()

            assertEquals(catalogue(version = 2), driver.storedCatalogue())
        }

    /** The source and the normaliser answer with [version] from now on. */
    private fun publish(version: Int) {
        source.lines = AppResult.Success(body("lines", version))
        source.stops = AppResult.Success(body("stops", version))
        normaliser.result = AppResult.Success(catalogue(version))
    }

    /** After a first sync, a refresh 25 h later normalises to [rejected], which must change nothing. */
    private suspend fun TestScope.assertRejectedAndCacheKept(rejected: StaticCatalogue) {
        val engine = engine()
        engine.syncIfStale()
        val firstSync = database.syncQueries.syncState(StaticDataset.LINES.key).executeAsOne()
        now += 25.hours
        source.lines = AppResult.Success(body("lines", 2))
        source.stops = AppResult.Success(body("stops", 2))
        normaliser.result = AppResult.Success(rejected)

        assertEquals(AppResult.Failure(AppError.Data), engine.syncIfStale())

        assertEquals(catalogue(), driver.storedCatalogue())
        // A rejected payload's hash is not kept either: the same payload must be checked again next time.
        assertEquals(firstSync, database.syncQueries.syncState(StaticDataset.LINES.key).executeAsOne())
    }

    private val nothing = StaticCatalogue(emptyList(), emptyList())
}
