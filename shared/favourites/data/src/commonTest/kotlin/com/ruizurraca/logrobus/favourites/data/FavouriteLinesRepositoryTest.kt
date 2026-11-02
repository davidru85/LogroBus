package com.ruizurraca.logrobus.favourites.data

import app.cash.turbine.test
import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.common.AppDispatchers
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.LogLevel
import com.ruizurraca.logrobus.core.database.FavouriteTypes
import com.ruizurraca.logrobus.core.database.logroBusDatabase
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** LB-103 (FR-FAV-01, FR-LINE-12, FR-LINE-15): the favourite lines, stored on the device in the user's order. */
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

            assertEquals(AppResult.Success(Unit), repository.add(10))
            assertEquals(AppResult.Success(Unit), repository.add(3))

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

            assertEquals(AppResult.Success(Unit), repository.add(10))

            repository.observeFavouriteLineIds().test {
                assertEquals(AppResult.Success(listOf(10, 3)), awaitItem())
            }
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
}
