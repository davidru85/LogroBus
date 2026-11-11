package com.ruizurraca.logrobus.android.feature.stops

import app.cash.turbine.test
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.favourites.domain.FAVOURITES_PER_TYPE
import com.ruizurraca.logrobus.favourites.domain.FavouriteAddition
import com.ruizurraca.logrobus.favourites.domain.SavedStop
import com.ruizurraca.logrobus.favourites.domain.SavedStopRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * LB-208 (FR-STOP-08, FR-FAV-05): the stop detail's Save action. It shows whether this stop is saved and under which
 * alias, saves it, removes it, renames it, and says the limit once when a save was refused.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SavedStopViewModelTest {
    private val saved = FakeSavedStops()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun lb208_anUnsavedStopShowsNoAlias_andASavedOneShowsItsAlias() =
        runTest {
            val viewModel = SavedStopViewModel(stopId = 38, saved = saved)

            viewModel.state.test {
                assertFalse(awaitItem().isSaved)

                saved.saved.value = listOf(SavedStop(stopId = 38, alias = "Casa", position = 0))

                val savedState = awaitItem()
                assertTrue(savedState.isSaved)
                assertEquals("Casa", savedState.alias)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun lb208_savingTheStopStoresItsAlias() =
        runTest {
            val viewModel = SavedStopViewModel(stopId = 38, saved = saved)

            viewModel.save(alias = "Casa")

            assertEquals(listOf(38 to "Casa"), saved.added)
        }

    @Test
    fun lb208_removingTheStopDropsIt() =
        runTest {
            val viewModel = SavedStopViewModel(stopId = 38, saved = saved)

            viewModel.remove()

            assertEquals(listOf(38), saved.removed)
        }

    @Test
    fun lb208_renamingKeepsTheStopSaved_underTheNewAlias() =
        runTest {
            val viewModel = SavedStopViewModel(stopId = 38, saved = saved)

            viewModel.rename(alias = "Piso")

            assertEquals(listOf(38 to "Piso"), saved.renamed)
        }

    @Test
    fun lb208_aRefusedSaveSaysTheLimitOnce() =
        runTest {
            saved.nextAddition = FavouriteAddition.LIMIT_REACHED
            val viewModel = SavedStopViewModel(stopId = 38, saved = saved)

            viewModel.save(alias = "Casa")
            assertTrue("the limit is reported", viewModel.limitReached.value)

            viewModel.limitMessageShown()
            assertFalse("and only once", viewModel.limitReached.value)
        }

    @Test
    fun lb208_aFailedSaveSaysNothing_andOffersTheActionAgain() =
        runTest {
            saved.failing = true
            val viewModel = SavedStopViewModel(stopId = 38, saved = saved)

            viewModel.save(alias = "Casa")

            assertFalse(viewModel.limitReached.value)
            assertNull("nothing is claimed saved", viewModel.state.value.alias)
        }

    @Test
    fun lb208_anotherStopsSaveIsNotThisStops() =
        runTest {
            saved.saved.value = listOf(SavedStop(stopId = 61, alias = "Trabajo", position = 0))
            val viewModel = SavedStopViewModel(stopId = 38, saved = saved)

            assertFalse(viewModel.state.value.isSaved)
            assertNull(viewModel.state.value.alias)
        }

    @Test
    fun lb208_theLimitIsTheFavouritesOne() {
        // The screen's message names the same 20 the repository enforces.
        assertEquals(20, FAVOURITES_PER_TYPE)
    }

    /** The saved stops in memory, as the graph's would be, with what each write was asked for. */
    private class FakeSavedStops : SavedStopRepository {
        val saved = MutableStateFlow<List<SavedStop>>(emptyList())
        /** The stop id and alias of each save, in order. */
        val added = mutableListOf<Pair<Int, String?>>()
        val renamed = mutableListOf<Pair<Int, String?>>()
        val removed = mutableListOf<Int>()

        var nextAddition: FavouriteAddition = FavouriteAddition.ADDED
        var failing = false

        // The flow follows the list, as the repository's does.
        override fun observeSavedStops(): Flow<AppResult<List<SavedStop>>> = saved.map { AppResult.Success(it) }

        override suspend fun add(
            stopId: Int,
            alias: String?,
        ): AppResult<FavouriteAddition> {
            if (failing) return AppResult.Failure(AppError.Data)
            added += stopId to alias
            return AppResult.Success(nextAddition)
        }

        override suspend fun rename(
            stopId: Int,
            alias: String?,
        ): AppResult<Unit> {
            if (failing) return AppResult.Failure(AppError.Data)
            renamed += stopId to alias
            return AppResult.Success(Unit)
        }

        override suspend fun remove(stopId: Int): AppResult<Unit> {
            if (failing) return AppResult.Failure(AppError.Data)
            removed += stopId
            return AppResult.Success(Unit)
        }

        override suspend fun reorder(stopIds: List<Int>): AppResult<Unit> = AppResult.Success(Unit)
    }
}
