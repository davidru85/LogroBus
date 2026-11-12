package com.ruizurraca.logrobus.android.feature.nearme

import app.cash.turbine.test
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.favourites.domain.FavouriteAddition
import com.ruizurraca.logrobus.favourites.domain.SavedStop
import com.ruizurraca.logrobus.favourites.domain.SavedStopRepository
import com.ruizurraca.logrobus.feature.nearme.domain.MyStops
import com.ruizurraca.logrobus.feature.nearme.domain.ObserveMyStops
import com.ruizurraca.logrobus.feature.nearme.presentation.SavedStopRowUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * LB-208 part 3 (FR-FAV-02, FR-STOP-08): the "My stops" edit sheet. It reorders, renames and deletes the saved stops,
 * and each change reaches the repository with the order and the alias the user made.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MyStopsEditViewModelTest {
    private val saved = FakeSavedStops()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun lb208_reorderingSendsTheUsersOrder() =
        runTest {
            saved.stops.value = listOf(stop(38, "Casa", 0), stop(61, "Trabajo", 1), stop(92, "Cole", 2))
            val viewModel = MyStopsEditViewModel(saved, saved.asUseCase())

            viewModel.reorder(listOf(92, 38, 61))

            assertEquals(listOf(listOf(92, 38, 61)), saved.reordered)
        }

    @Test
    fun lb208_renamingSendsTheNewAlias() =
        runTest {
            saved.stops.value = listOf(stop(38, "Casa", 0))
            val viewModel = MyStopsEditViewModel(saved, saved.asUseCase())

            viewModel.rename(stopId = 38, alias = "Piso")

            assertEquals(listOf(38 to "Piso"), saved.renamed)
        }

    @Test
    fun lb208_deletingRemovesTheStop() =
        runTest {
            saved.stops.value = listOf(stop(38, "Casa", 0))
            val viewModel = MyStopsEditViewModel(saved, saved.asUseCase())

            viewModel.delete(stopId = 38)

            assertEquals(listOf(38), saved.removed)
        }

    @Test
    fun lb208_theSheetShowsTheRowsInTheUsersOrder() =
        runTest {
            saved.stops.value = listOf(stop(92, "Cole", 0), stop(38, "Casa", 1))
            val viewModel = MyStopsEditViewModel(saved, saved.asUseCase())

            viewModel.rows.test {
                assertEquals(listOf(92, 38), awaitItem().map { it.stopId })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun lb208_aStopTheCatalogueLostCanStillBeDeleted() =
        runTest {
            // No alias and no name: the row still exists, so the user can remove it rather than be stuck with it.
            saved.stops.value = listOf(stop(99, null, 0))
            val viewModel = MyStopsEditViewModel(saved, saved.asUseCase())

            viewModel.rows.test {
                assertEquals(1, awaitItem().size)
                cancelAndIgnoreRemainingEvents()
            }

            viewModel.delete(stopId = 99)
            assertEquals(listOf(99), saved.removed)
        }

    @Test
    fun lb208_aFailedWriteLeavesTheRowsAsTheyWere() =
        runTest {
            saved.stops.value = listOf(stop(38, "Casa", 0))
            saved.failing = true
            val viewModel = MyStopsEditViewModel(saved, saved.asUseCase())

            viewModel.delete(stopId = 38)

            viewModel.rows.test {
                assertEquals(listOf(38), awaitItem().map { it.stopId })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun lb208_theOrderHelperNeverMovesPastEitherEnd() {
        // The sheet's buttons call this; a first row has no "up" action and a last none for "down", but the helper must
        // also hold if one is ever wired to the wrong end.
        val rows =
            listOf(
                SavedStopRowUi(stopId = 38, label = "Casa", position = 0),
                SavedStopRowUi(stopId = 61, label = "Trabajo", position = 1),
            )

        // The first row has no "up" and the last none for "down"; the helper holds either way.
        assertEquals(listOf(38, 61), rows.moved(0, -1))
        assertEquals(listOf(38, 61), rows.moved(1, 1))
        assertEquals(listOf(61, 38), rows.moved(0, 1))
        assertEquals(emptyList<Int>(), emptyList<SavedStopRowUi>().moved(0, 1))
    }

    private fun stop(
        id: Int,
        alias: String?,
        position: Int,
    ) = SavedStop(stopId = id, alias = alias, position = position)

    /** The saved stops in memory, with what each write was asked for. */
    private class FakeSavedStops : SavedStopRepository {
        val stops = MutableStateFlow<List<SavedStop>>(emptyList())
        val reordered = mutableListOf<List<Int>>()
        val renamed = mutableListOf<Pair<Int, String?>>()
        val removed = mutableListOf<Int>()
        var failing = false

        /** The rows' source, over the same list, as the graph's use case would give them. */
        fun asUseCase() = ObserveMyStops { stops.map { list -> AppResult.Success(MyStops(list, emptyMap())) } }

        override fun observeSavedStops(): Flow<AppResult<List<SavedStop>>> = stops.map { AppResult.Success(it) }

        override suspend fun add(
            stopId: Int,
            alias: String?,
        ): AppResult<FavouriteAddition> = AppResult.Success(FavouriteAddition.ADDED)

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

        override suspend fun reorder(stopIds: List<Int>): AppResult<Unit> {
            if (failing) return AppResult.Failure(AppError.Data)
            reordered += stopIds
            return AppResult.Success(Unit)
        }
    }
}
