package com.ruizurraca.logrobus.android.feature.stops

import app.cash.turbine.test
import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.preferences.EtaFormat
import com.ruizurraca.logrobus.core.preferences.PreferencesStore
import com.ruizurraca.logrobus.core.preferences.ThemePreference
import com.ruizurraca.logrobus.core.preferences.UserPreferences
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.core.presentation.EtaDisplay
import com.ruizurraca.logrobus.feature.stops.domain.ObserveStopArrivals
import com.ruizurraca.logrobus.feature.stops.domain.ObserveStopDetail
import com.ruizurraca.logrobus.feature.stops.domain.StopArrivalRow
import com.ruizurraca.logrobus.feature.stops.domain.StopArrivals
import com.ruizurraca.logrobus.feature.stops.presentation.StopDetailUiMapper
import com.ruizurraca.logrobus.feature.stops.presentation.StopDetailUiState
import com.ruizurraca.logrobus.transit.domain.Direction
import com.ruizurraca.logrobus.transit.domain.ServedLine
import com.ruizurraca.logrobus.transit.domain.Stop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * LB-205 (FR-STOP-03 to FR-STOP-05, FR-ERR-06) and ADR-0007: the stop detail's ViewModel builds its state with the
 * shared use cases and mapper, asks API-06 for the stop's served lines only, re-words the list on a format change
 * without asking again, and loads again on Retry.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StopDetailViewModelTest {
    private val now = Instant.parse("2026-11-10T11:00:00Z")

    private val stop =
        Stop(
            id = 38,
            name = "Gonzalo de Berceo",
            lat = 42.4650,
            lng = -2.4456,
            isOperational = true,
            isProvisional = false,
            servedLines =
                listOf(
                    ServedLine(1, "1", "Hospital San Pedro", Direction.OUTBOUND, 0xFFE30613L, 0xFFFFFFFFL),
                    ServedLine(33, "33", "El Arco", Direction.INBOUND, 0xFF1E7A3FL, 0xFFFFFFFFL),
                ),
        )

    private val answer =
        StopArrivals(
            stop = stop,
            arrivals =
                listOf(
                    StopArrivalRow(
                        lineCode = "1",
                        color = 0xFFE30613L,
                        onColor = 0xFFFFFFFFL,
                        headsign = "Hospital San Pedro",
                        expectedAt = now + 4.minutes,
                        isCancelled = false,
                        isLowConfidence = false,
                    ),
                ),
            fetchedAt = now,
            lastError = null,
        )

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun preferences(saved: MutableStateFlow<UserPreferences>) =
        object : PreferencesStore {
            override val preferences = saved

            override suspend fun setTheme(theme: ThemePreference) = AppResult.Success(Unit)

            override suspend fun setEtaFormat(format: EtaFormat) = AppResult.Success(Unit)
        }

    @Test
    fun lb205_theDetailAsksForTheStopsServedLines_andShowsWhatTheMapperMakesOfTheAnswer() =
        runTest {
            val asked = mutableListOf<List<Int>>()
            val viewModel =
                StopDetailViewModel(
                    stopId = 38,
                    observeDetail = ObserveStopDetail { flowOf(AppResult.Success(stop)) },
                    observeArrivals = { _, lineIds ->
                        asked += lineIds
                        flowOf(AppResult.Success(answer))
                    },
                    preferences = preferences(MutableStateFlow(UserPreferences())),
                    clock = AppClock { now },
                    nightServiceText = { language -> if (language == "es") "Noche en español" else "Night in English" },
                )

            viewModel.state.test {
                // A synchronous fake answers on subscribe, so the first state a collector sees is the answer: the
                // Loading that `stateIn` starts with is replaced before the test can read it.
                assertEquals(
                    StopDetailUiMapper.state(AppResult.Success(answer), 38, now, EtaFormat.MINUTES),
                    awaitSettled(),
                )
                cancelAndIgnoreRemainingEvents()
            }
            // FR-STOP-03: API-06 is asked with the stop's served lines and nothing else.
            assertEquals(listOf(listOf(1, 33)), asked)
        }

    @Test
    fun lb205_aNewEtaFormatReWordsTheListAlreadyReceived_withoutAskingAgain() =
        runTest {
            val saved = MutableStateFlow(UserPreferences())
            var asks = 0
            val viewModel =
                StopDetailViewModel(
                    stopId = 38,
                    observeDetail = ObserveStopDetail { flowOf(AppResult.Success(stop)) },
                    observeArrivals = { _, _ ->
                        asks++
                        flowOf(AppResult.Success(answer))
                    },
                    preferences = preferences(saved),
                    clock = AppClock { now },
                    nightServiceText = { language -> if (language == "es") "Noche en español" else "Night in English" },
                )

            viewModel.state.test {
                val minutes = awaitSettled() as StopDetailUiState.Content
                assertEquals(EtaDisplay.Minutes(4), minutes.arrivals.single().eta)

                saved.value = UserPreferences(etaFormat = EtaFormat.CLOCK_TIME)

                val clockTime = awaitItem() as StopDetailUiState.Content
                assertEquals(
                    EtaDisplay.of(now + 4.minutes, now, EtaFormat.CLOCK_TIME),
                    clockTime.arrivals.single().eta,
                )
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(1, asks)
        }

    @Test
    fun lb205_retryLoadsTheStopAgain() =
        runTest {
            val answers =
                ArrayDeque(
                    listOf(flowOf(AppResult.Failure(AppError.Network)), flowOf(AppResult.Success(answer))),
                )
            val viewModel =
                StopDetailViewModel(
                    stopId = 38,
                    observeDetail = ObserveStopDetail { flowOf(AppResult.Success(stop)) },
                    observeArrivals = ObserveStopArrivals { _, _ -> answers.removeFirst() },
                    preferences = preferences(MutableStateFlow(UserPreferences())),
                    clock = AppClock { now },
                    nightServiceText = { language -> if (language == "es") "Noche en español" else "Night in English" },
                )

            viewModel.state.test {
                assertEquals(StopDetailUiState.Error(ErrorReason.NETWORK), awaitSettled())

                viewModel.retry()

                assertEquals(
                    StopDetailUiMapper.state(AppResult.Success(answer), 38, now, EtaFormat.MINUTES),
                    awaitSettled(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    /** The first state that is not Loading, since a tick may re-emit it. */
    private suspend fun app.cash.turbine.ReceiveTurbine<StopDetailUiState>.awaitSettled(): StopDetailUiState {
        var state = awaitItem()
        while (state == StopDetailUiState.Loading) state = awaitItem()
        return state
    }
}
