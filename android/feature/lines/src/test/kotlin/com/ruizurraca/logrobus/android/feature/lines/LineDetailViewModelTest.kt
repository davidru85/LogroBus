package com.ruizurraca.logrobus.android.feature.lines

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
import com.ruizurraca.logrobus.feature.lines.domain.FavouriteToggle
import com.ruizurraca.logrobus.feature.lines.domain.LinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLineArrivals
import com.ruizurraca.logrobus.feature.lines.domain.ObserveLinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.domain.SetFavouriteLine
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailEvent
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailUiMapper
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailUiState
import com.ruizurraca.logrobus.transit.domain.Arrival
import com.ruizurraca.logrobus.transit.domain.Direction
import com.ruizurraca.logrobus.transit.domain.Line
import com.ruizurraca.logrobus.transit.domain.LineKind
import com.ruizurraca.logrobus.transit.domain.LinePattern
import com.ruizurraca.logrobus.transit.domain.LineStop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

/**
 * LB-103 (FR-LINE-09) and ADR-0007: the line detail's ViewModel builds its state with the shared use case and mapper,
 * and its toolbar's toggle sets the line as a favourite.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LineDetailViewModelTest {
    private val line3 = Line(3, "3", "Campillo - El Cubo", 0xFFF9A825, 0xFF000000, LineKind.DAY, isOperational = true)
    private val lines = AppResult.Success(LinesWithFavourites(listOf(line3), favouriteIds = emptyList()))

    /** The ViewModel with the LB-105 collaborators: no calls, the default format, and a stopped clock. */
    private fun viewModel(
        observeLines: ObserveLinesWithFavourites,
        observeArrivals: ObserveLineArrivals = { _, _ -> flowOf(AppResult.Success(emptyList())) },
        clock: AppClock = AppClock { Instant.parse("2026-11-08T11:00:00Z") },
        preferences: PreferencesStore = defaultPreferences(),
        setFavourite: SetFavouriteLine,
    ) = LineDetailViewModel(
        lineId = 3,
        observeLines = observeLines,
        observeArrivals = observeArrivals,
        preferences = preferences,
        clock = clock,
        setFavourite = setFavourite,
    )

    /** The defaults of the LB-105 collaborators: no calls, the user's untouched settings, and a stopped clock. */
    private fun defaultPreferences(): PreferencesStore =
        object : PreferencesStore {
            override val preferences = flowOf(UserPreferences())

            override suspend fun setTheme(theme: ThemePreference) = AppResult.Success(Unit)

            override suspend fun setEtaFormat(format: EtaFormat) = AppResult.Success(Unit)
        }

    private val lineWithStops =
        line3.copy(
            headsigns = listOf("El Cubo", "Campillo"),
            patterns =
                mapOf(
                    Direction.OUTBOUND to
                        LinePattern(
                            headsign = "El Cubo",
                            stops =
                                listOf(
                                    LineStop(stopId = 38, name = "Campillo", lat = 42.45, lng = -2.44),
                                    LineStop(stopId = 39, name = "El Cubo", lat = 42.46, lng = -2.43),
                                ),
                        ),
                    Direction.INBOUND to
                        LinePattern(
                            headsign = "Campillo",
                            stops = listOf(LineStop(stopId = 39, name = "El Cubo", lat = 42.46, lng = -2.43)),
                        ),
                ),
        )

    /** One call of line 3 at [stopId], expected at [expectedAt]. */
    private fun arrival(
        stopId: Int,
        expectedAt: Instant,
    ) = Arrival(lineId = 3, stopId = stopId, direction = Direction.OUTBOUND, expectedAt = expectedAt)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun lb103_theDetailLoads_thenShowsWhatTheSharedMapperMakesOfItsLine() =
        runTest {
            val observed = MutableSharedFlow<AppResult<LinesWithFavourites>>()
            val viewModel =
                viewModel({ observed }) { _, _ -> AppResult.Success(FavouriteToggle.Done) }

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
                viewModel({ flowOf(lines) }) { lineId, isFavourite ->
                    set += lineId to isFavourite
                    AppResult.Success(FavouriteToggle.Done)
                }

            viewModel.onEvent(LineDetailEvent.SetFavourite(isFavourite = true))
            viewModel.onEvent(LineDetailEvent.SetFavourite(isFavourite = false))

            assertEquals(listOf(3 to true, 3 to false), set)
        }

    @Test
    fun lb110_aToggleBeyondTheLimitShowsTheLimit_untilTheMessageHasBeenShown() =
        runTest {
            val viewModel =
                viewModel({ flowOf(lines) }) { _, _ ->
                    AppResult.Success(FavouriteToggle.LimitReached(20))
                }
            assertEquals(null, viewModel.favouriteLimit.value)

            viewModel.onEvent(LineDetailEvent.SetFavourite(isFavourite = true))
            assertEquals(20, viewModel.favouriteLimit.value)

            viewModel.onEvent(LineDetailEvent.FavouriteLimitShown)
            assertEquals(null, viewModel.favouriteLimit.value)
        }

    @Test
    fun lb103_retryLoadsTheLineAgain() =
        runTest {
            val loads = ArrayDeque(listOf(flowOf(AppResult.Failure(AppError.Network)), flowOf(lines)))
            val observe = ObserveLinesWithFavourites { loads.removeFirst() }
            val viewModel = viewModel(observe) { _, _ -> AppResult.Success(FavouriteToggle.Done) }

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
                viewModel({
                    flow {
                        delay(100)
                        emit(lines)
                    }
                }) { _, _ -> AppResult.Success(FavouriteToggle.Done) }
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

    @Test
    fun lb105_eachStopShowsItsNextCall_andAStopWithoutOneKeepsTheDash() =
        runTest {
            val now = Instant.parse("2026-11-08T11:00:00Z")
            val calls =
                listOf(
                    arrival(stopId = 38, expectedAt = now + 4.minutes),
                    arrival(stopId = 38, expectedAt = now + 9.minutes),
                )
            val viewModel =
                viewModel(
                    {
                        flowOf(
                            AppResult.Success(LinesWithFavourites(listOf(lineWithStops), favouriteIds = emptyList())),
                        )
                    },
                    observeArrivals = { _, _ -> flowOf(AppResult.Success(calls)) },
                    setFavourite = { _, _ -> AppResult.Success(FavouriteToggle.Done) },
                )

            backgroundScope.launch { viewModel.state.collect {} }
            viewModel.arrivals.test {
                val byStop = awaitItem()
                assertEquals(setOf(38, 39), byStop.keys)
                assertEquals(EtaDisplay.Minutes(4), byStop.getValue(38).eta)
                assertEquals("a stop with no call keeps the criterion's dash", null, byStop.getValue(39).eta)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun lb105_theCallsAreAskedForTheDirectionTheDetailShows_andNotTheOther() =
        runTest {
            val asked = mutableListOf<Direction?>()
            val viewModel =
                viewModel(
                    {
                        flowOf(
                            AppResult.Success(LinesWithFavourites(listOf(lineWithStops), favouriteIds = emptyList())),
                        )
                    },
                    observeArrivals = { _, direction ->
                        asked += direction
                        flowOf(AppResult.Success(emptyList()))
                    },
                    setFavourite = { _, _ -> AppResult.Success(FavouriteToggle.Done) },
                )

            backgroundScope.launch { viewModel.state.collect {} }
            backgroundScope.launch { viewModel.arrivals.collect {} }
            advanceUntilIdle()
            assertEquals(listOf<Direction?>(Direction.OUTBOUND), asked)

            viewModel.onEvent(LineDetailEvent.SelectDirection(Direction.INBOUND))
            advanceUntilIdle()
            assertEquals(listOf(Direction.OUTBOUND, Direction.INBOUND), asked)
        }

    @Test
    fun lb105_theUserFormatRewordsTheCallsAlreadyReceived_withoutAskingAgain() =
        runTest {
            val now = Instant.parse("2026-11-08T11:00:00Z")
            val saved = MutableStateFlow(UserPreferences())
            var asks = 0
            val viewModel =
                viewModel(
                    {
                        flowOf(
                            AppResult.Success(LinesWithFavourites(listOf(lineWithStops), favouriteIds = emptyList())),
                        )
                    },
                    observeArrivals = { _, _ ->
                        asks++
                        flowOf(AppResult.Success(listOf(arrival(stopId = 38, expectedAt = now + 4.minutes))))
                    },
                    preferences =
                        object : PreferencesStore {
                            override val preferences = saved

                            override suspend fun setTheme(theme: ThemePreference) = AppResult.Success(Unit)

                            override suspend fun setEtaFormat(format: EtaFormat) = AppResult.Success(Unit)
                        },
                    setFavourite = { _, _ -> AppResult.Success(FavouriteToggle.Done) },
                )

            backgroundScope.launch { viewModel.state.collect {} }
            backgroundScope.launch { viewModel.arrivals.collect {} }
            advanceUntilIdle()
            assertEquals(EtaDisplay.Minutes(4), viewModel.arrivals.value[38]?.eta)

            saved.value = UserPreferences(etaFormat = EtaFormat.CLOCK_TIME)
            advanceUntilIdle()
            assertTrue(
                "FR-SET-01: the clock form is the same instant, worded as a time",
                viewModel.arrivals.value[38]?.eta is EtaDisplay.ClockTime,
            )
            assertEquals("FR-SET-01: a format change re-words the calls it already has", 1, asks)
        }

    @Test
    fun lb105_changingTheDirectionDropsTheOldOnesCalls_beforeTheNewOnesArrive() =
        runTest {
            // Both patterns number their stops separately, but the poller emits nothing until its first fetch
            // returns; the rows must not paint the direction just left behind in the meantime.
            val now = Instant.parse("2026-11-08T11:00:00Z")
            val viewModel =
                viewModel(
                    {
                        flowOf(
                            AppResult.Success(LinesWithFavourites(listOf(lineWithStops), favouriteIds = emptyList())),
                        )
                    },
                    observeArrivals = { _, direction ->
                        if (direction == Direction.OUTBOUND) {
                            flowOf(AppResult.Success(listOf(arrival(stopId = 38, expectedAt = now + 4.minutes))))
                        } else {
                            flowOf() // A collection that has not answered yet.
                        }
                    },
                    setFavourite = { _, _ -> AppResult.Success(FavouriteToggle.Done) },
                )

            backgroundScope.launch { viewModel.state.collect {} }
            backgroundScope.launch { viewModel.arrivals.collect {} }
            advanceUntilIdle()
            assertEquals(EtaDisplay.Minutes(4), viewModel.arrivals.value[38]?.eta)

            viewModel.onEvent(LineDetailEvent.SelectDirection(Direction.INBOUND))
            advanceUntilIdle()
            assertTrue(
                "the inbound rows show no call, never the outbound times",
                viewModel.arrivals.value.values
                    .all { it.eta == null },
            )
        }

    private suspend fun app.cash.turbine.ReceiveTurbine<LineDetailUiState>.awaitSettled(): LineDetailUiState {
        var state = awaitItem()
        while (state == LineDetailUiState.Loading) state = awaitItem()
        return state
    }
}
