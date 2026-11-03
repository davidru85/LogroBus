package com.ruizurraca.logrobus.android.feature.lines

import app.cash.turbine.test
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.feature.lines.domain.FavouriteToggle
import com.ruizurraca.logrobus.feature.lines.domain.LinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailEvent
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailUiMapper
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailUiState
import com.ruizurraca.logrobus.transit.domain.Line
import com.ruizurraca.logrobus.transit.domain.LineKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
 * LB-103 (FR-LINE-09) and ADR-0007: the line detail's ViewModel builds its state with the shared use case and mapper,
 * and its toolbar's toggle sets the line as a favourite.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LineDetailViewModelTest {
    private val line3 = Line(3, "3", "Campillo - El Cubo", 0xFFF9A825, 0xFF000000, LineKind.DAY, isOperational = true)
    private val lines = AppResult.Success(LinesWithFavourites(listOf(line3), favouriteIds = emptyList()))

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun lb103_theDetailLoads_thenShowsWhatTheSharedMapperMakesOfItsLine() =
        runTest {
            val observed = MutableSharedFlow<AppResult<LinesWithFavourites>>()
            val viewModel =
                LineDetailViewModel(lineId = 3, { observed }) { _, _ -> AppResult.Success(FavouriteToggle.Done) }

            viewModel.state.test {
                assertEquals(LineDetailUiState.Loading, awaitItem())
                observed.emit(lines)
                assertEquals(LineDetailUiMapper.state(lines, lineId = 3), awaitItem())
            }
        }

    @Test
    fun lb103_theToolbarsToggleSetsTheLineAsAFavourite_orNot() =
        runTest {
            val set = mutableListOf<Pair<Int, Boolean>>()
            val viewModel =
                LineDetailViewModel(lineId = 3, { flowOf(lines) }) { lineId, isFavourite ->
                    set += lineId to isFavourite
                    AppResult.Success(FavouriteToggle.Done)
                }

            viewModel.onEvent(LineDetailEvent.SetFavourite(isFavourite = true))
            viewModel.onEvent(LineDetailEvent.SetFavourite(isFavourite = false))

            assertEquals(listOf(3 to true, 3 to false), set)
        }

    @Test
    fun lb103_retryLoadsTheLineAgain() =
        runTest {
            val loads = ArrayDeque(listOf(flowOf(AppResult.Failure(AppError.Network)), flowOf(lines)))
            val observe = ObserveLinesWithFavourites { loads.removeFirst() }
            val viewModel = LineDetailViewModel(lineId = 3, observe) { _, _ -> AppResult.Success(FavouriteToggle.Done) }

            viewModel.state.test {
                assertEquals(LineDetailUiState.Error(ErrorReason.NETWORK), awaitSettled())

                viewModel.onEvent(LineDetailEvent.Retry)

                assertEquals(LineDetailUiMapper.state(lines, lineId = 3), awaitSettled())
            }
        }

    @Test
    fun lb103_returningToTheDetailAfterARetryKeepsTheLine_withoutShowingLoadingAgain() =
        runTest {
            // As the Lines list (LB-101): after the upstream stops, coming back reads again, and the line must stay.
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val viewModel =
                LineDetailViewModel(
                    lineId = 3,
                    {
                        flow {
                            delay(100)
                            emit(lines)
                        }
                    },
                ) { _, _ -> AppResult.Success(FavouriteToggle.Done) }
            viewModel.onEvent(LineDetailEvent.Retry)
            val states = mutableListOf<LineDetailUiState>()
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

            assertEquals(listOf(LineDetailUiMapper.state(lines, lineId = 3)), states)
        }

    private suspend fun app.cash.turbine.ReceiveTurbine<LineDetailUiState>.awaitSettled(): LineDetailUiState {
        var state = awaitItem()
        while (state == LineDetailUiState.Loading) state = awaitItem()
        return state
    }
}
