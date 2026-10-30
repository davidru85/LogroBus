package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.common.AppDispatchers
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.LogLevel
import com.ruizurraca.logrobus.core.database.FavouriteTypes
import com.ruizurraca.logrobus.core.database.LogroBusDatabase
import com.ruizurraca.logrobus.core.database.logroBusDatabase
import com.ruizurraca.logrobus.core.database.searchStops
import com.ruizurraca.logrobus.transit.domain.StaticDataset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
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

    private fun TestScope.engine(database: LogroBusDatabase = this@StaticSyncEngineTest.database) =
        StandardTestDispatcher(testScheduler).let { dispatcher ->
            DefaultStaticSyncEngine(
                source = source,
                normaliser = normaliser,
                database = database,
                clock = AppClock { now },
                dispatchers = AppDispatchers(main = dispatcher, default = dispatcher, io = dispatcher),
                logger = logger,
            )
        }

    @AfterTest
    fun closeDatabase() = driver.close()

    @Test
    fun lb008_aNewDatabaseHasNoLinesOrStops() {
        // ADR-0008: no seed is bundled or imported; the first sync brings everything.
        assertEquals(nothing, driver.storedCatalogue())
    }

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
    fun lb008_aSuccessfulSyncRecordsWhenEachDatasetSyncedAndTheHashOfItsContent() =
        runTest {
            engine().syncIfStale()

            // The content: the normaliser's fingerprint, then the body as decoded and encoded again.
            val lines = database.syncQueries.syncState(StaticDataset.LINES.key).executeAsOne()
            val stops = database.syncQueries.syncState(StaticDataset.STOPS.key).executeAsOne()
            assertEquals(
                now to sha256Hex("normaliser 1\n${body("lines", 1)}"),
                lines.last_success to lines.content_hash,
            )
            assertEquals(
                now to sha256Hex("normaliser 1\n${body("stops", 1)}"),
                stops.last_success to stops.content_hash,
            )
        }

    @Test
    fun lb008_aChangedNormaliserRenormalisesAnUnchangedPayload() =
        runTest {
            // DD-11: new name overrides, or a normaliser fixed in an app update, change what the same payload stores.
            val engine = engine()
            engine.syncIfStale()
            now += 25.hours
            normaliser.fingerprint = "normaliser 2"
            normaliser.result = AppResult.Success(catalogue(version = 2))

            engine.syncIfStale()

            assertEquals(catalogue(version = 2), driver.storedCatalogue())
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

    @Test
    fun lb008_aWriteThatFailsHalfwayRollsBackAndKeepsTheCache() =
        runTest {
            val engine = engine()
            engine.syncIfStale()
            val firstSync = database.syncQueries.syncState(StaticDataset.LINES.key).executeAsOne()
            now += 25.hours
            publish(version = 2)
            // The lines are written before the stops, and the last stop repeats the first one's id.
            val payload = catalogue(version = 2)
            normaliser.result = AppResult.Success(payload.copy(stops = payload.stops + payload.stops.first()))

            assertEquals(AppResult.Failure(AppError.Data), engine.syncIfStale())

            assertEquals(catalogue(), driver.storedCatalogue())
            assertEquals(firstSync, database.syncQueries.syncState(StaticDataset.LINES.key).executeAsOne())
            assertTrue(logger.entries.any { it.level == LogLevel.Warn && it.tag == "StaticSync" })
        }

    @Test
    fun lb008_observersHearOfTheNewCatalogueOnceItIsWhole() =
        runTest {
            val engine = engine()
            engine.syncIfStale()
            var notifications = 0
            driver.addListener("line", "pattern", "pattern_stop", "stop", "stop_line") { notifications++ }
            now += 25.hours
            publish(version = 2)

            engine.syncIfStale()

            // One transaction, so a reader sees the old catalogue or the new one, never a mix of both.
            assertEquals(1, notifications)
        }

    @Test
    fun lb008_aSavedStopMissingFromASyncCountsOneMoreMissingSync() =
        runTest {
            val engine = engine()
            save(FavouriteTypes.STOP, "5")
            save(FavouriteTypes.STOP, "101")
            engine.syncIfStale()
            now += 25.hours
            publish(version = 2)

            engine.syncIfStale()

            assertEquals(mapOf("5" to 0L, "101" to 2L), missingSyncs(FavouriteTypes.STOP))
        }

    @Test
    fun lb008_aSavedStopThatReappearsStartsAgainFromZero() =
        runTest {
            val engine = engine()
            save(FavouriteTypes.STOP, "101")
            engine.syncIfStale()
            assertEquals(mapOf("101" to 1L), missingSyncs(FavouriteTypes.STOP))
            now += 25.hours
            publish(version = 2)
            normaliser.result = AppResult.Success(catalogue(version = 2, stopCount = FEWEST_STOPS + 1))

            engine.syncIfStale()

            assertEquals(mapOf("101" to 0L), missingSyncs(FavouriteTypes.STOP))
        }

    @Test
    fun lb008_anUnchangedPayloadStillCountsMissingStops() =
        runTest {
            // FR-FAV-04 counts daily syncs: a source that stops changing must still reach "No longer served".
            val engine = engine()
            save(FavouriteTypes.STOP, "101")
            engine.syncIfStale()
            now += 25.hours

            engine.syncIfStale()

            assertEquals(mapOf("101" to 2L), missingSyncs(FavouriteTypes.STOP))
        }

    @Test
    fun lb008_aSavedStopThatIsOnlyProvisionalIsMissing() =
        runTest {
            // DD-10: a provisional stop is made up from a pattern, so API-02 no longer delivers it.
            val payload = catalogue(stopCount = FEWEST_STOPS + 1)
            normaliser.result =
                AppResult.Success(payload.copy(stops = payload.stops.map { it.copy(isProvisional = it.id == 5) }))
            save(FavouriteTypes.STOP, "5")

            engine().syncIfStale()

            assertEquals(mapOf("5" to 1L), missingSyncs(FavouriteTypes.STOP))
        }

    @Test
    fun lb008_aFailedSyncCountsNoMissingSync() =
        runTest {
            save(FavouriteTypes.STOP, "101")
            source.lines = AppResult.Failure(AppError.Network)

            engine().syncIfStale()

            assertEquals(mapOf("101" to 0L), missingSyncs(FavouriteTypes.STOP))
        }

    @Test
    fun lb008_onlySavedStopsAreCounted() =
        runTest {
            save("line", "101")

            engine().syncIfStale()

            assertEquals(mapOf("101" to 0L), missingSyncs("line"))
        }

    private fun save(
        type: String,
        refId: String,
    ) {
        driver.execute(
            identifier = null,
            sql = "INSERT INTO favourite (id, type, ref_id, position, created_at) VALUES (?, ?, ?, 0, 0)",
            parameters = 3,
        ) {
            bindString(0, "$type $refId")
            bindString(1, type)
            bindString(2, refId)
        }
    }

    private fun missingSyncs(type: String): Map<String, Long> =
        driver
            .select("SELECT ref_id, missing_syncs, type FROM favourite") {
                Triple(it.getString(0)!!, it.getLong(1)!!, it.getString(2))
            }.filter { it.third == type }
            .associate { it.first to it.second }

    @Test
    fun lb008_aDatabaseThatCannotBeReadFailsWithData() =
        runTest {
            val closed = testDriver().also { it.close() }

            assertEquals(AppResult.Failure(AppError.Data), engine(database = logroBusDatabase(closed)).syncIfStale())
        }

    @Test
    fun lb008_aNormaliserThatThrowsFailsWithDataAndKeepsTheCache() =
        runTest {
            engine().syncIfStale()
            now += 25.hours
            publish(version = 2)

            normaliser.failure = IllegalStateException("A bug in the normaliser")

            val result = engine().syncIfStale()

            assertEquals(AppResult.Failure(AppError.Data), result)
            assertEquals(catalogue(), driver.storedCatalogue())
        }

    @OptIn(ExperimentalCoroutinesApi::class) // advanceUntilIdle
    @Test
    fun lb008_twoSyncsAtOnceShareOneDownload() =
        runTest {
            // App start and the first screen can both ask for the catalogue before it is there.
            val engine = engine()
            source.gate = CompletableDeferred()
            val first = async { engine.syncIfStale() }
            val second = async { engine.syncIfStale() }
            advanceUntilIdle()

            source.gate?.complete(Unit)

            assertEquals(
                listOf(AppResult.Success(Unit), AppResult.Success(Unit)),
                listOf(first.await(), second.await()),
            )
            assertEquals(2, source.requests)
        }

    @Test
    fun lb008_whatLeavesThePayloadLeavesTheCacheAndTheSearch() =
        runTest {
            val engine = engine()
            normaliser.result = AppResult.Success(catalogue(lineCount = FEWEST_LINES + 1, stopCount = FEWEST_STOPS + 1))
            engine.syncIfStale()
            now += 25.hours

            publish(version = 2)
            engine.syncIfStale()

            assertEquals(catalogue(version = 2), driver.storedCatalogue())
            assertEquals(emptyList(), database.searchStops("colon 101").map { it.id })
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
