package com.ruizurraca.logrobus.android.feature.nearme

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.ruizurraca.logrobus.feature.nearme.domain.LocationAccess
import com.ruizurraca.logrobus.feature.nearme.domain.ObserveLocationAccess
import com.ruizurraca.logrobus.feature.nearme.presentation.NearMeEvent
import com.ruizurraca.logrobus.feature.nearme.presentation.NearMeUiMapper
import com.ruizurraca.logrobus.feature.nearme.presentation.NearMeUiState
import com.ruizurraca.logrobus.feature.nearme.presentation.NearYouUi
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
 * ADR-0007 and FR-LOC-01 to 03: the ViewModel shows what the shared mapper makes of location access, reads it again
 * when the permission may have changed, and stops the location updates after Near Me leaves.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NearMeViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun lb201_untilAccessIsKnownNothingShows_thenTheSharedMappersState() =
        runTest {
            val access = MutableSharedFlow<LocationAccess>()
            val viewModel = NearMeViewModel { access }

            viewModel.state.test {
                assertEquals(NearMeUiState(NearYouUi.Unknown), awaitItem())
                access.emit(LocationAccess.Off)
                assertEquals(NearMeUiMapper.state(LocationAccess.Off), awaitItem())
            }
        }

    @Test
    fun lb201_afterThePermissionMayHaveChanged_accessIsReadAgain() =
        runTest {
            val viewModel = NearMeViewModel(Scripted(flowOf(LocationAccess.Off), flowOf(LocationAccess.On)))

            viewModel.state.test {
                assertEquals(NearMeUiState(NearYouUi.TurnOnLocation), awaitSettled())

                viewModel.onEvent(NearMeEvent.PermissionMayHaveChanged)

                assertEquals(NearMeUiState(NearYouUi.LocationOn), awaitItem())
            }
        }

    @Test
    fun lb201_whileLocationIsOn_aCheckLeavesTheUpdatesRunning() =
        runTest {
            val access = MutableSharedFlow<LocationAccess>()
            val observe = Scripted(access)
            val viewModel = NearMeViewModel(observe)

            viewModel.state.test {
                awaitItem()
                access.emit(LocationAccess.On)
                assertEquals(NearMeUiState(NearYouUi.LocationOn), awaitItem())

                // The user is back from the system's settings, say. A revoked permission ends the flow by itself.
                viewModel.onEvent(NearMeEvent.PermissionMayHaveChanged)

                assertEquals(1, observe.calls)
                assertEquals(1, access.subscriptionCount.value)
            }
        }

    @Test
    fun lb201_untilAccessIsKnown_aCheckWaitsForTheReadUnderWay() =
        runTest {
            val access = MutableSharedFlow<LocationAccess>()
            val observe = Scripted(access, flowOf(LocationAccess.Off))
            val viewModel = NearMeViewModel(observe)

            viewModel.state.test {
                assertEquals(NearMeUiState(NearYouUi.Unknown), awaitItem())

                // The screen resumes as it opens, while the first read is under way.
                viewModel.onEvent(NearMeEvent.PermissionMayHaveChanged)

                assertEquals(1, observe.calls)
            }
        }

    @Test
    fun lb201_leavingNearMeStopsTheLocationUpdates_fiveSecondsLater() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val access = MutableSharedFlow<LocationAccess>()
            val viewModel = NearMeViewModel { access }

            val screen = backgroundScope.launch { viewModel.state.collect {} }
            runCurrent()
            assertEquals(1, access.subscriptionCount.value)

            // Another tab is selected, or the app goes to the background (FR-LOC-03). A configuration change takes
            // less than this, so the updates survive it.
            screen.cancel()
            advanceTimeBy(4_999)
            runCurrent()
            assertEquals(1, access.subscriptionCount.value)

            advanceTimeBy(2)
            runCurrent()
            assertEquals(0, access.subscriptionCount.value)
        }

    /** The next state after Unknown. A read may be quicker than the collector, which then never sees Unknown. */
    private suspend fun ReceiveTurbine<NearMeUiState>.awaitSettled(): NearMeUiState {
        var state = awaitItem()
        while (state.nearYou == NearYouUi.Unknown) state = awaitItem()
        return state
    }

    /** Hands out [flows] in order, one per call: a first read, then each check. */
    private class Scripted(
        vararg flows: Flow<LocationAccess>,
    ) : ObserveLocationAccess {
        private val reads = ArrayDeque(flows.toList())
        var calls = 0
            private set

        override fun invoke(): Flow<LocationAccess> {
            calls++
            return reads.removeFirst()
        }
    }
}
