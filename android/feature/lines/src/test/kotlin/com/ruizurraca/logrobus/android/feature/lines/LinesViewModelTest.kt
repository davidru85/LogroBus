package com.ruizurraca.logrobus.android.feature.lines

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.feature.lines.domain.FavouriteToggle
import com.ruizurraca.logrobus.feature.lines.domain.LinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.domain.ReorderFavouriteLines
import com.ruizurraca.logrobus.feature.lines.domain.SetFavouriteLine
import com.ruizurraca.logrobus.feature.lines.presentation.LinesEvent
import com.ruizurraca.logrobus.feature.lines.presentation.LinesUiMapper
import com.ruizurraca.logrobus.feature.lines.presentation.LinesUiState
import com.ruizurraca.logrobus.transit.domain.Line
import com.ruizurraca.logrobus.transit.domain.LineKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * LB-101, ADR-0007 and testing_strategy.md §3: the Lines ViewModel builds every state with the shared use case and
 * mapper, retries on Retry, and stops its upstream after the screen leaves. LB-103: a row's toggle sets its line as a
 * favourite. LB-102: the search field's text filters the lines. LB-110: the edit sheet's new order is saved, and a
 * toggle beyond the limit shows the limit message once.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LinesViewModelTest {
    private val lines =
        AppResult.Success(
            LinesWithFavourites(
                listOf(
                    Line(4, "4", "Casa Blanca - Yagüe", 0xFFE30613, 0xFFFFFFFF, LineKind.DAY, isOperational = true),
                    Line(101, "B1", "Varea - La Cava", 0xFFE30613, 0xFFFFFFFF, LineKind.NIGHT, isOperational = true),
                ),
                favouriteIds = emptyList(),
            ),
        )

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun lb101_theViewModelLoads_thenShowsWhatTheSharedMapperMakesOfTheLines() =
        runTest {
            val observed = MutableSharedFlow<AppResult<LinesWithFavourites>>()
            val viewModel = viewModel { observed }

            viewModel.state.test {
                assertEquals(LinesUiState.Loading, awaitItem())
                observed.emit(lines)
                assertEquals(LinesUiMapper.state(lines), awaitItem())
            }
        }

    @Test
    fun lb101_aFirstLaunchWithNoNetworkShowsTheError_andRetryLoadsTheLines() =
        runTest {
            val viewModel = viewModel(observe = Scripted(flowOf(AppResult.Failure(AppError.Network)), flowOf(lines)))

            viewModel.state.test {
                assertEquals(LinesUiState.Error(ErrorReason.NETWORK), awaitSettled())

                viewModel.onEvent(LinesEvent.Retry)

                assertEquals(LinesUiMapper.state(lines), awaitSettled())
            }
        }

    @Test
    fun lb101_leavingTheScreenStopsTheUseCase_fiveSecondsLater() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val observed = MutableSharedFlow<AppResult<LinesWithFavourites>>()
            val viewModel = viewModel { observed }

            val screen = backgroundScope.launch { viewModel.state.collect {} }
            runCurrent()
            assertEquals(1, observed.subscriptionCount.value)

            // A configuration change takes less than this, so the list survives it.
            screen.cancel()
            advanceTimeBy(4_999)
            runCurrent()
            assertEquals(1, observed.subscriptionCount.value)

            advanceTimeBy(2)
            runCurrent()
            assertEquals(0, observed.subscriptionCount.value)
        }

    @Test
    fun lb101_returningToTheScreenKeepsTheList_withoutShowingLoadingAgain() =
        runTest {
            // After the upstream stops (5 s away, in More, another tab or the background), coming back reads again.
            // The list must stay, as on iOS: a skeleton would flash and the list would lose its scroll position.
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            // A read that takes a moment, as the database's does.
            val viewModel =
                viewModel {
                    flow {
                        delay(100)
                        emit(lines)
                    }
                }
            val states = mutableListOf<LinesUiState>()
            backgroundScope.launch { viewModel.state.collect { states += it } }.let { first ->
                advanceTimeBy(200)
                runCurrent()
                first.cancel()
            }
            advanceTimeBy(6_000)
            runCurrent()
            states.clear()

            backgroundScope.launch { viewModel.state.collect { states += it } }
            advanceTimeBy(200)
            runCurrent()

            assertEquals(listOf(LinesUiMapper.state(lines)), states)
        }

    @Test
    fun lb103_aRowsToggleSetsItsLineAsAFavourite_orNot() =
        runTest {
            // FR-LINE-15: the carousel changes through the use case's flow, at once.
            val set = mutableListOf<Pair<Int, Boolean>>()
            val viewModel =
                viewModel(
                    setFavourite = { lineId, isFavourite ->
                        set += lineId to isFavourite
                        AppResult.Success(FavouriteToggle.Done)
                    },
                ) { flowOf(lines) }

            viewModel.onEvent(LinesEvent.SetFavourite(lineId = 3, isFavourite = true))
            viewModel.onEvent(LinesEvent.SetFavourite(lineId = 10, isFavourite = false))

            assertEquals(listOf(3 to true, 10 to false), set)
        }

    @Test
    fun lb102_aSearchFiltersTheList_andClearingItBringsTheListBack() =
        runTest {
            // FR-LINE-03, FR-LINE-16: the shared mapper makes the results of the search field's text.
            val viewModel = viewModel { flowOf(lines) }

            viewModel.state.test {
                assertEquals(LinesUiMapper.state(lines), awaitSettled())

                viewModel.onEvent(LinesEvent.Search("varea"))
                assertEquals(LinesUiMapper.state(lines, query = "varea"), awaitItem())

                viewModel.onEvent(LinesEvent.Search(""))
                assertEquals(LinesUiMapper.state(lines), awaitItem())
            }
        }

    @Test
    fun lb102_aSearchFiltersTheLinesItHas_withoutLoadingThemAgain() =
        runTest {
            // One load: a second call of the use case would start a sync, and Loading would flash at each key.
            val viewModel = viewModel(observe = Scripted(flowOf(lines)))

            viewModel.state.test {
                awaitSettled()
                // "b" finds both lines (Blanca, B1), and "b1" only the night one.
                viewModel.onEvent(LinesEvent.Search("b"))
                assertEquals(LinesUiMapper.state(lines, query = "b"), awaitItem())

                viewModel.onEvent(LinesEvent.Search("b1"))
                assertEquals(LinesUiMapper.state(lines, query = "b1"), awaitItem())
            }
        }

    @Test
    fun lb102_aSearchThatFindsNothingShowsNoMatches() =
        runTest {
            val viewModel = viewModel { flowOf(lines) }

            viewModel.state.test {
                awaitSettled()
                viewModel.onEvent(LinesEvent.Search("tranvia"))

                assertEquals(LinesUiState.NoMatches, awaitItem())
            }
        }

    @Test
    fun lb102_aSearchTypedBeforeTheLinesArriveFiltersThem() =
        runTest {
            val observed = MutableSharedFlow<AppResult<LinesWithFavourites>>()
            val viewModel = viewModel { observed }

            viewModel.state.test {
                assertEquals(LinesUiState.Loading, awaitItem())
                viewModel.onEvent(LinesEvent.Search("varea"))
                observed.emit(lines)

                assertEquals(LinesUiMapper.state(lines, query = "varea"), awaitItem())
            }
        }

    @Test
    fun lb102_aRetryKeepsTheSearch() =
        runTest {
            val viewModel = viewModel(observe = Scripted(flowOf(AppResult.Failure(AppError.Network)), flowOf(lines)))

            viewModel.state.test {
                awaitSettled()
                viewModel.onEvent(LinesEvent.Search("varea"))
                viewModel.onEvent(LinesEvent.Retry)

                assertEquals(LinesUiMapper.state(lines, query = "varea"), awaitSettled())
            }
        }

    @Test
    fun lb110_theEditSheetsNewOrderIsSaved() =
        runTest {
            val reordered = mutableListOf<List<Int>>()
            val viewModel =
                viewModel(
                    reorderFavourites = { lineIds ->
                        reordered += lineIds
                        AppResult.Success(Unit)
                    },
                ) { flowOf(lines) }

            viewModel.onEvent(LinesEvent.ReorderFavourites(listOf(101, 4)))

            assertEquals(listOf(listOf(101, 4)), reordered)
        }

    @Test
    fun lb110_aToggleBeyondTheLimitShowsTheLimit_untilTheMessageHasBeenShown() =
        runTest {
            // FR-FAV-05: the row's toggle stays off, and the screen says why, once.
            val viewModel =
                viewModel(setFavourite = {
                    _,
                    _,
                    ->
                    AppResult.Success(FavouriteToggle.LimitReached(20))
                }) { flowOf(lines) }
            assertEquals(null, viewModel.favouriteLimit.value)

            viewModel.onEvent(LinesEvent.SetFavourite(lineId = 4, isFavourite = true))
            assertEquals(20, viewModel.favouriteLimit.value)

            viewModel.onEvent(LinesEvent.FavouriteLimitShown)
            assertEquals(null, viewModel.favouriteLimit.value)
        }

    @Test
    fun lb110_aToggleThatIsDoneShowsNoLimit() =
        runTest {
            val viewModel = viewModel { flowOf(lines) }

            viewModel.onEvent(LinesEvent.SetFavourite(lineId = 4, isFavourite = true))

            assertEquals(null, viewModel.favouriteLimit.value)
        }

    private fun viewModel(
        setFavourite: SetFavouriteLine = SetFavouriteLine { _, _ -> AppResult.Success(FavouriteToggle.Done) },
        reorderFavourites: ReorderFavouriteLines = ReorderFavouriteLines { AppResult.Success(Unit) },
        observe: ObserveLinesWithFavourites,
    ) = LinesViewModel(observe, setFavourite, reorderFavourites)

    /** The next state after Loading. A read may be quicker than the collector, which then never sees Loading. */
    private suspend fun ReceiveTurbine<LinesUiState>.awaitSettled(): LinesUiState {
        var state = awaitItem()
        while (state == LinesUiState.Loading) state = awaitItem()
        return state
    }

    /** Hands out [flows] in order, one per call: a first load, then each Retry. */
    private class Scripted(
        vararg flows: Flow<AppResult<LinesWithFavourites>>,
    ) : ObserveLinesWithFavourites {
        private val loads = ArrayDeque(flows.toList())

        override fun invoke() = loads.removeFirst()
    }
}
