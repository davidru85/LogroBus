package com.ruizurraca.logrobus.android.feature.settings

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.feature.settings.domain.DatasetSync
import com.ruizurraca.logrobus.feature.settings.domain.ObserveLastSyncs
import com.ruizurraca.logrobus.feature.settings.presentation.AboutDataEvent
import com.ruizurraca.logrobus.feature.settings.presentation.AboutDataUiMapper
import com.ruizurraca.logrobus.feature.settings.presentation.AboutDataUiState
import com.ruizurraca.logrobus.transit.domain.StaticDataset
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
import kotlin.time.Instant

/**
 * ADR-0007 and testing_strategy.md §3: an Android ViewModel builds every state with the shared use case and mapper,
 * handles each event, and stops its upstream after the screen leaves.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AboutDataViewModelTest {
    private val synced =
        AppResult.Success(
            listOf(
                DatasetSync(StaticDataset.LINES, Instant.parse("2026-10-28T09:30:00Z")),
                DatasetSync(StaticDataset.STOPS, null),
            ),
        )

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun lb010_theViewModelLoads_thenShowsWhatTheSharedMapperMakesOfTheUseCase() =
        runTest {
            val lastSyncs = MutableSharedFlow<AppResult<List<DatasetSync>>>()
            val viewModel = AboutDataViewModel { lastSyncs }

            viewModel.state.test {
                assertEquals(AboutDataUiState.Loading, awaitItem())
                lastSyncs.emit(synced)
                assertEquals(AboutDataUiMapper.state(synced), awaitItem())
            }
        }

    @Test
    fun lb010_aFailureShowsTheErrorState_andRetryReadsAgain() =
        runTest {
            val viewModel = AboutDataViewModel(Scripted(flowOf(AppResult.Failure(AppError.Data)), flowOf(synced)))

            viewModel.state.test {
                assertEquals(AboutDataUiState.Error(ErrorReason.DATA), awaitSettled())

                viewModel.onEvent(AboutDataEvent.Retry)

                assertEquals(AboutDataUiMapper.state(synced), awaitSettled())
            }
        }

    @Test
    fun lb010_leavingTheScreenStopsTheUseCase_fiveSecondsLater() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val lastSyncs = MutableSharedFlow<AppResult<List<DatasetSync>>>()
            val viewModel = AboutDataViewModel { lastSyncs }

            val screen = backgroundScope.launch { viewModel.state.collect {} }
            runCurrent()
            assertEquals(1, lastSyncs.subscriptionCount.value)

            // A configuration change takes less than this, so the query survives it.
            screen.cancel()
            advanceTimeBy(4_999)
            runCurrent()
            assertEquals(1, lastSyncs.subscriptionCount.value)

            advanceTimeBy(2)
            runCurrent()
            assertEquals(0, lastSyncs.subscriptionCount.value)
        }

    /** The next state after Loading. A read may be quicker than the collector, which then never sees Loading. */
    private suspend fun ReceiveTurbine<AboutDataUiState>.awaitSettled(): AboutDataUiState {
        var state = awaitItem()
        while (state == AboutDataUiState.Loading) state = awaitItem()
        return state
    }

    /** Hands out [flows] in order, one per call: a first read, then each Retry. */
    private class Scripted(
        vararg flows: Flow<AppResult<List<DatasetSync>>>,
    ) : ObserveLastSyncs {
        private val reads = ArrayDeque(flows.toList())

        override fun invoke() = reads.removeFirst()
    }
}
