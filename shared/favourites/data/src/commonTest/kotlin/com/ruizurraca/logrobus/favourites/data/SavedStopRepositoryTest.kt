package com.ruizurraca.logrobus.favourites.data

import app.cash.turbine.test
import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.common.AppDispatchers
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.LogLevel
import com.ruizurraca.logrobus.core.database.FavouriteTypes
import com.ruizurraca.logrobus.core.database.logroBusDatabase
import com.ruizurraca.logrobus.favourites.domain.FAVOURITES_PER_TYPE
import com.ruizurraca.logrobus.favourites.domain.FavouriteAddition
import com.ruizurraca.logrobus.favourites.domain.SavedStop
import com.ruizurraca.logrobus.favourites.domain.SavedStopAlias
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

/**
 * LB-208 (FR-STOP-08, FR-FAV-01, FR-FAV-02, FR-FAV-05): the saved stops, with their alias of at most 30 characters, in
 * the user's order, at most 20, kept across restarts.
 */
class SavedStopRepositoryTest {
    private val driver = testDriver()
    private val database = logroBusDatabase(driver)
    private val logger = RecordingLogger()

    private fun TestScope.repository() =
        SavedStopRepositoryImpl(
            database = database,
            dispatchers = AppDispatchers(StandardTestDispatcher(testScheduler)),
            clock = AppClock { Instant.parse("2026-11-11T10:00:00Z") },
            logger = logger,
        )

    @AfterTest
    fun closeDatabase() = driver.close()

    @Test
    fun lb208_noStopsAreSavedAtFirst() =
        runTest {
            repository().observeSavedStops().test {
                assertEquals(AppResult.Success(emptyList<SavedStop>()), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun lb208_aSavedStopKeepsItsAliasAndItsPlace() =
        runTest {
            val repository = repository()

            assertEquals(AppResult.Success(FavouriteAddition.ADDED), repository.add(stopId = 38, alias = "Casa"))

            repository.observeSavedStops().test {
                assertEquals(
                    listOf(SavedStop(stopId = 38, alias = "Casa", position = 0)),
                    (awaitItem() as AppResult.Success).value,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun lb208_anAliasLongerThanThirtyCharactersIsCutToThirty() =
        runTest {
            val repository = repository()

            val alias = "A".repeat(45)
            repository.add(stopId = 38, alias = alias)

            repository.observeSavedStops().test {
                assertEquals(
                    30,
                    (awaitItem() as AppResult.Success)
                        .value
                        .single()
                        .alias
                        ?.length,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun lb208_aSavedStopWithoutAnAliasKeepsNone() =
        runTest {
            val repository = repository()

            repository.add(stopId = 38, alias = "   ")

            repository.observeSavedStops().test {
                assertEquals(null, (awaitItem() as AppResult.Success).value.single().alias)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun lb208_savingAStopThatIsAlreadySaved_changesNothing() =
        runTest {
            val repository = repository()
            repository.add(stopId = 38, alias = "Casa")

            val second = repository.add(stopId = 38, alias = "Trabajo")

            assertEquals(AppResult.Success(FavouriteAddition.ALREADY_SAVED), second)
            repository.observeSavedStops().test {
                // The first alias stands: a double tap must not silently rename what the user saved.
                assertEquals(listOf("Casa"), (awaitItem() as AppResult.Success).value.map { it.alias })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun lb208_aTwentyFirstSavedStopIsRefused_andNothingIsAdded() =
        runTest {
            val repository = repository()
            repeat(FAVOURITES_PER_TYPE) { index -> repository.add(stopId = index + 1, alias = null) }

            val refused = repository.add(stopId = 999, alias = "Casa")

            assertEquals(AppResult.Success(FavouriteAddition.LIMIT_REACHED), refused)
            repository.observeSavedStops().test {
                assertEquals(FAVOURITES_PER_TYPE, (awaitItem() as AppResult.Success).value.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun lb208_renamingASavedStopKeepsItsPlace() =
        runTest {
            val repository = repository()
            repository.add(stopId = 38, alias = "Casa")
            repository.add(stopId = 61, alias = "Trabajo")

            assertEquals(AppResult.Success(Unit), repository.rename(stopId = 38, alias = "Piso"))

            repository.observeSavedStops().test {
                assertEquals(
                    listOf(SavedStop(38, "Piso", 0), SavedStop(61, "Trabajo", 1)),
                    (awaitItem() as AppResult.Success).value,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun lb208_reorderingPutsTheStopsInTheUsersOrder() =
        runTest {
            val repository = repository()
            repository.add(stopId = 38, alias = "Casa")
            repository.add(stopId = 61, alias = "Trabajo")
            repository.add(stopId = 92, alias = "Cole")

            assertEquals(AppResult.Success(Unit), repository.reorder(listOf(92, 38, 61)))

            repository.observeSavedStops().test {
                assertEquals(listOf(92, 38, 61), (awaitItem() as AppResult.Success).value.map { it.stopId })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun lb208_removingASavedStopLeavesTheRestInOrder() =
        runTest {
            val repository = repository()
            repository.add(stopId = 38, alias = "Casa")
            repository.add(stopId = 61, alias = "Trabajo")

            assertEquals(AppResult.Success(Unit), repository.remove(stopId = 38))

            repository.observeSavedStops().test {
                assertEquals(listOf(61), (awaitItem() as AppResult.Success).value.map { it.stopId })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun lb208_theAliasIsCutByItsCodePoints_notByItsUtf16Units() {
        // Forty glyphs, each two UTF-16 units, plus a "C": 41 code points but 81 units. Thirty characters is the "C"
        // plus twenty-nine whole glyphs; a cut by units would take thirty and end on the fifteenth glyph's high
        // surrogate, which is half a character no screen can draw.
        val emoji = "C" + "\uD83D\uDE8C".repeat(40)

        val alias = SavedStopAlias.of(emoji)!!

        assertEquals(30, alias.codePointCount(0, alias.length), "the field's thirty characters")
        val endsMidGlyph = alias.last().code in 0xD800..0xDBFF
        assertEquals(false, endsMidGlyph, "the cut never splits a glyph")
    }

    @Test
    fun lb208_aSavedStopIsStoredUnderTheStopsOwnType() =
        runTest {
            repository().add(stopId = 38, alias = "Casa")

            val refs =
                database.favouriteQueries.selectFavouriteRefs(FavouriteTypes.STOP).executeAsList()

            assertEquals(listOf("38"), refs, "a saved stop never shares the favourite lines' type")
        }
}
