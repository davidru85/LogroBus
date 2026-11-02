package com.ruizurraca.logrobus.android.feature.lines

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLines
import com.ruizurraca.logrobus.feature.lines.presentation.LinesEvent
import com.ruizurraca.logrobus.feature.lines.presentation.LinesUiMapper
import com.ruizurraca.logrobus.feature.lines.presentation.LinesUiState
import com.ruizurraca.logrobus.transit.domain.Line
import com.ruizurraca.logrobus.transit.domain.LineKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
 * mapper, retries on Retry, and stops its upstream after the screen leaves.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LinesViewModelTest {
    private val lines =
        AppResult.Success(
            listOf(
                Line(4, "4", "Casa Blanca - Yagüe", 0xFFE30613, 0xFFFFFFFF, LineKind.DAY, isOperational = true),
                Line(101, "B1", "Varea - La Cava", 0xFFE30613, 0xFFFFFFFF, LineKind.NIGHT, isOperational = true),
            ),
        )

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun lb101_theViewModelLoads_thenShowsWhatTheSharedMapperMakesOfTheLines() =
        runTest {
            val observed = MutableSharedFlow<AppResult<List<Line>>>()
            val viewModel = LinesViewModel { observed }

            viewModel.state.test {
                assertEquals(LinesUiState.Loading, awaitItem())
                observed.emit(lines)
                assertEquals(LinesUiMapper.state(lines), awaitItem())
            }
        }

    @Test
    fun lb101_aFirstLaunchWithNoNetworkShowsTheError_andRetryLoadsTheLines() =
        runTest {
            val viewModel = LinesViewModel(Scripted(flowOf(AppResult.Failure(AppError.Network)), flowOf(lines)))

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
            val observed = MutableSharedFlow<AppResult<List<Line>>>()
            val viewModel = LinesViewModel { observed }

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

    /** The next state after Loading. A read may be quicker than the collector, which then never sees Loading. */
    private suspend fun ReceiveTurbine<LinesUiState>.awaitSettled(): LinesUiState {
        var state = awaitItem()
        while (state == LinesUiState.Loading) state = awaitItem()
        return state
    }

    /** Hands out [flows] in order, one per call: a first load, then each Retry. */
    private class Scripted(
        vararg flows: Flow<AppResult<List<Line>>>,
    ) : ObserveLines {
        private val loads = ArrayDeque(flows.toList())

        override fun invoke() = loads.removeFirst()
    }
}
