package com.ruizurraca.logrobus.favourites.data

import app.cash.turbine.test
import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.common.AppDispatchers
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.LogLevel
import com.ruizurraca.logrobus.core.database.FavouriteTypes
import com.ruizurraca.logrobus.core.database.logroBusDatabase
import com.ruizurraca.logrobus.favourites.domain.FAVOURITES_PER_TYPE
import com.ruizurraca.logrobus.favourites.domain.FavouriteAddition
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * LB-103 (FR-FAV-01, FR-LINE-12, FR-LINE-15): the favourite lines, stored on the device in the user's order. LB-110
 * (FR-FAV-02, FR-FAV-05): the user reorders them, and saves at most 20.
 */
class FavouriteLinesRepositoryTest {
    private val driver = testDriver()
    private val database = logroBusDatabase(driver)
    private val logger = RecordingLogger()

    private fun TestScope.repository() =
        FavouriteLinesRepositoryImpl(
            database = database,
            dispatchers = AppDispatchers(StandardTestDispatcher(testScheduler)),
            clock = AppClock { Instant.parse("2026-11-02T10:00:00Z") },
            logger = logger,
        )

    @AfterTest
    fun closeDatabase() = driver.close()

    @Test
    fun lb103_thereAreNoFavouriteLinesAtFirst() =
        runTest {
            repository().observeFavouriteLineIds().test {
                assertEquals(AppResult.Success(emptyList()), awaitItem())
            }
        }

    @Test
    fun lb103_aLineAddedGoesAtTheEnd() =
        runTest {
            val repository = repository()

            assertEquals(AppResult.Success(FavouriteAddition.ADDED), repository.add(10))
            assertEquals(AppResult.Success(FavouriteAddition.ADDED), repository.add(3))

            repository.observeFavouriteLineIds().test {
                assertEquals(AppResult.Success(listOf(10, 3)), awaitItem())
            }
        }

    @Test
    fun lb103_aChangeIsEmittedAtOnce() =
        runTest {
            val repository = repository()

            repository.observeFavouriteLineIds().test {
                assertEquals(AppResult.Success(emptyList()), awaitItem())
                repository.add(3)
                assertEquals(AppResult.Success(listOf(3)), awaitItem())
                repository.remove(3)
                assertEquals(AppResult.Success(emptyList()), awaitItem())
            }
        }

    @Test
    fun lb103_aRemovedLineLeavesTheOthersInTheirOrder() =
        runTest {
            val repository = repository()
            listOf(1, 10, 3).forEach { repository.add(it) }

            assertEquals(AppResult.Success(Unit), repository.remove(10))
            repository.add(10)

            repository.observeFavouriteLineIds().test {
                assertEquals(AppResult.Success(listOf(1, 3, 10)), awaitItem())
            }
        }

    @Test
    fun lb103_addingALineThatIsAlreadyAFavouriteKeepsItWhereItWas() =
        runTest {
            val repository = repository()
            listOf(10, 3).forEach { repository.add(it) }

            // Nothing was saved: a double tap must not count favourite_add twice (NFR-OBS-03).
            assertEquals(AppResult.Success(FavouriteAddition.ALREADY_SAVED), repository.add(10))

            repository.observeFavouriteLineIds().test {
                assertEquals(AppResult.Success(listOf(10, 3)), awaitItem())
            }
        }

    @Test
    fun lb103_changesApplyInTheOrderTheyWereMade() =
        runTest {
            // Two quick taps: a stale "add", then "remove". The writes run on a pool of threads, so without an order of
            // their own the later one could land first. This dispatcher runs the newest write first.
            val io = NewestFirstDispatcher()
            val repository =
                FavouriteLinesRepositoryImpl(
                    database,
                    AppDispatchers(main = io, default = io, io = io),
                    AppClock { Instant.parse("2026-11-02T10:00:00Z") },
                    logger,
                )

            val add = launch(start = CoroutineStart.UNDISPATCHED) { repository.add(3) }
            val remove = launch(start = CoroutineStart.UNDISPATCHED) { repository.remove(3) }
            while (add.isActive || remove.isActive) {
                io.runNewestFirst()
                runCurrent()
            }

            repository().observeFavouriteLineIds().test {
                assertEquals(AppResult.Success(emptyList()), awaitItem())
            }
        }

    @Test
    fun lb103_aSavedLineWhoseIdIsNotANumberIsDropped_andCountedInTheLog() =
        runTest {
            // Only this module writes line favourites, so such a row is damage: dropped, and logged by count only.
            val yesterday = Instant.parse("2026-11-01T10:00:00Z")
            database.favouriteQueries.insertFavourite("line:x", FavouriteTypes.LINE, "x", yesterday)
            val repository = repository()
            repository.add(10)

            repository.observeFavouriteLineIds().test {
                assertEquals(AppResult.Success(listOf(10)), awaitItem())
            }
            assertEquals(listOf("Dropped 1 favourite line with a malformed id"), logger.entries.map { it.message })
        }

    @Test
    fun lb103_theFavouritesAndTheirOrderSurviveARestart() =
        runTest {
            listOf(10, 3, 101).forEach { repository().add(it) }

            // A new repository over the same database, as after a restart.
            repository().observeFavouriteLineIds().test {
                assertEquals(AppResult.Success(listOf(10, 3, 101)), awaitItem())
            }
        }

    @Test
    fun lb103_savedStopsAreNotLines() =
        runTest {
            database.favouriteQueries.insertFavourite(
                id = "stop:3",
                type = FavouriteTypes.STOP,
                refId = "3",
                createdAt = Instant.parse("2026-11-01T10:00:00Z"),
            )
            val repository = repository()
            repository.add(10)

            repository.observeFavouriteLineIds().test {
                assertEquals(AppResult.Success(listOf(10)), awaitItem())
            }
        }

    @Test
    fun lb103_aDatabaseThatCannotBeUsedGivesAFailure_notACrash() =
        runTest {
            driver.execute(null, "DROP TABLE favourite", 0)
            val repository = repository()

            assertEquals(AppResult.Failure(AppError.Data), repository.add(3))
            assertEquals(AppResult.Failure(AppError.Data), repository.remove(3))
            repository.observeFavouriteLineIds().test {
                assertEquals(AppResult.Failure(AppError.Data), awaitItem())
                awaitComplete()
            }
            assertEquals(listOf(LogLevel.Warn), logger.entries.map { it.level }.distinct())
        }

    @Test
    fun lb110_aLineBeyondTheLimitIsNotSaved() =
        runTest {
            val repository = repository()
            val twenty = (1..FAVOURITES_PER_TYPE).toList()
            twenty.forEach { assertEquals(AppResult.Success(FavouriteAddition.ADDED), repository.add(it)) }

            assertEquals(AppResult.Success(FavouriteAddition.LIMIT_REACHED), repository.add(101))

            repository.observeFavouriteLineIds().test {
                assertEquals(AppResult.Success(twenty), awaitItem())
            }
        }

    @Test
    fun lb110_aLineAlreadySavedAtTheLimitIsAlreadySaved_notBeyondIt() =
        runTest {
            val repository = repository()
            (1..FAVOURITES_PER_TYPE).forEach { repository.add(it) }

            assertEquals(AppResult.Success(FavouriteAddition.ALREADY_SAVED), repository.add(5))
        }

    @Test
    fun lb110_savedStopsDoNotCountTowardsTheLimitOfLines() =
        runTest {
            // FR-FAV-05: the limit is per type (requirements.md §3.6).
            (1..FAVOURITES_PER_TYPE).forEach { stop ->
                database.favouriteQueries.insertFavourite(
                    "stop:$stop",
                    FavouriteTypes.STOP,
                    "$stop",
                    Instant.DISTANT_PAST,
                )
            }

            assertEquals(AppResult.Success(FavouriteAddition.ADDED), repository().add(10))
        }

    @Test
    fun lb110_aNewOrderIsSaved_andSurvivesARestart() =
        runTest {
            val repository = repository()
            listOf(10, 3, 101).forEach { repository.add(it) }

            assertEquals(AppResult.Success(Unit), repository.reorder(listOf(101, 10, 3)))

            repository().observeFavouriteLineIds().test {
                assertEquals(AppResult.Success(listOf(101, 10, 3)), awaitItem())
            }
        }

    @Test
    fun lb110_aNewOrderKeepsTheLinesItLeavesOut_afterTheOthers() =
        runTest {
            // A line saved from another screen after the edit sheet read the favourites keeps its place, last.
            val repository = repository()
            listOf(10, 3, 101, 4).forEach { repository.add(it) }

            repository.reorder(listOf(3, 10))

            repository.observeFavouriteLineIds().test {
                assertEquals(AppResult.Success(listOf(3, 10, 101, 4)), awaitItem())
            }
        }

    @Test
    fun lb110_aNewOrderSkipsLinesThatAreNoLongerSaved_andTheNextLineGoesLast() =
        runTest {
            val repository = repository()
            listOf(10, 3).forEach { repository.add(it) }

            repository.reorder(listOf(3, 99, 10))
            repository.add(5)

            repository.observeFavouriteLineIds().test {
                assertEquals(AppResult.Success(listOf(3, 10, 5)), awaitItem())
            }
        }

    @Test
    fun lb110_aNewOrderIsOneChange() =
        runTest {
            // The carousel must not show the half-moved order of a reorder that is still being written.
            val repository = repository()
            listOf(10, 3, 101).forEach { repository.add(it) }

            repository.observeFavouriteLineIds().test {
                assertEquals(AppResult.Success(listOf(10, 3, 101)), awaitItem())
                repository.reorder(listOf(101, 3, 10))
                assertEquals(AppResult.Success(listOf(101, 3, 10)), awaitItem())
                expectNoEvents()
            }
        }

    @Test
    fun lb110_aNewOrderThatCannotBeSavedGivesAFailure_notACrash() =
        runTest {
            driver.execute(null, "DROP TABLE favourite", 0)

            assertEquals(AppResult.Failure(AppError.Data), repository().reorder(listOf(3, 10)))
            assertEquals(listOf(LogLevel.Warn), logger.entries.map { it.level })
        }
}

/** Holds what it is given, and runs the newest first: the worst order for writes that a pool of threads can give. */
private class NewestFirstDispatcher : CoroutineDispatcher() {
    private val queued = ArrayDeque<Runnable>()

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        queued.addLast(block)
    }

    fun runNewestFirst() {
        while (queued.isNotEmpty()) queued.removeLast().run()
    }
}
