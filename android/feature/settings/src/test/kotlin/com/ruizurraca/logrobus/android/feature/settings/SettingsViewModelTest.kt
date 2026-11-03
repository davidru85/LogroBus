package com.ruizurraca.logrobus.android.feature.settings

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.preferences.EtaFormat
import com.ruizurraca.logrobus.core.preferences.ThemePreference
import com.ruizurraca.logrobus.core.preferences.UserPreferences
import com.ruizurraca.logrobus.feature.settings.domain.DatasetSync
import com.ruizurraca.logrobus.feature.settings.presentation.AppLanguage
import com.ruizurraca.logrobus.feature.settings.presentation.CacheMessage
import com.ruizurraca.logrobus.feature.settings.presentation.SettingsEvent
import com.ruizurraca.logrobus.feature.settings.presentation.SettingsUiMapper
import com.ruizurraca.logrobus.feature.settings.presentation.SettingsUiState
import com.ruizurraca.logrobus.transit.domain.StaticDataset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
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
 * ADR-0007 and testing_strategy.md §3: Settings' ViewModel builds its state with the shared use cases and mapper, sends
 * each change where it belongs, and says once that the cache was cleared (LB-403, FR-SET-01).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val synced =
        AppResult.Success(listOf(DatasetSync(StaticDataset.LINES, Instant.parse("2026-11-03T09:30:00Z"))))
    private val preferences = MutableStateFlow(UserPreferences())
    private val lastSyncs = MutableStateFlow<AppResult<List<DatasetSync>>>(synced)
    private val languages = FakeAppLanguages()
    private var cacheClear: AppResult<Unit> = AppResult.Success(Unit)
    private var cacheClears = 0

    private fun viewModel() =
        SettingsViewModel(
            observePreferences = { preferences },
            observeLastSyncs = { lastSyncs },
            setTheme = { theme ->
                preferences.update { saved -> saved.copy(theme = theme) }
                AppResult.Success(Unit)
            },
            setEtaFormat = { format ->
                preferences.update { saved -> saved.copy(etaFormat = format) }
                AppResult.Success(Unit)
            },
            clearCache = { cacheClear.also { if (it is AppResult.Success) cacheClears++ } },
            languages = languages,
        )

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun lb403_theViewModelShowsWhatTheSharedMapperMakesOfTheSettings() =
        runTest {
            viewModel().state.test {
                assertEquals(
                    SettingsUiMapper.state(UserPreferences(), synced, AppLanguage.SYSTEM),
                    awaitSettled(),
                )
            }
        }

    @Test
    fun lb403_aNewThemeAndEtaFormat_goThroughTheirUseCases_andShow() =
        runTest {
            val viewModel = viewModel()

            viewModel.state.test {
                awaitSettled()

                viewModel.onEvent(SettingsEvent.SetTheme(ThemePreference.DARK))
                assertEquals(ThemePreference.DARK, (awaitItem() as SettingsUiState.Content).theme)

                viewModel.onEvent(SettingsEvent.SetEtaFormat(EtaFormat.CLOCK_TIME))
                assertEquals(EtaFormat.CLOCK_TIME, (awaitItem() as SettingsUiState.Content).etaFormat)
            }
        }

    @Test
    fun lb403_aNewLanguage_goesToAndroid_andShows() =
        runTest {
            val viewModel = viewModel()

            viewModel.state.test {
                awaitSettled()

                viewModel.onEvent(SettingsEvent.SetLanguage(AppLanguage.ENGLISH))

                assertEquals(AppLanguage.ENGLISH, languages.current.value)
                assertEquals(AppLanguage.ENGLISH, (awaitItem() as SettingsUiState.Content).language)
            }
        }

    @Test
    fun lb403_clearCache_clearsIt_andSaysSoOnce() =
        runTest {
            val viewModel = viewModel()

            viewModel.cacheMessage.test {
                assertEquals(null, awaitItem())

                viewModel.onEvent(SettingsEvent.ClearCache)
                assertEquals(CacheMessage.CLEARED, awaitItem())
                assertEquals(1, cacheClears)

                viewModel.onEvent(SettingsEvent.CacheMessageShown)
                assertEquals(null, awaitItem())
            }
        }

    @Test
    fun lb403_aCacheThatCannotBeCleared_saysSo() =
        runTest {
            cacheClear = AppResult.Failure(AppError.Data)
            val viewModel = viewModel()

            viewModel.onEvent(SettingsEvent.ClearCache)

            assertEquals(CacheMessage.FAILED, viewModel.cacheMessage.value)
        }

    @Test
    fun lb403_leavingTheScreenStopsReadingTheSettings_fiveSecondsLater() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val viewModel = viewModel()

            val screen = backgroundScope.launch { viewModel.state.collect {} }
            runCurrent()
            assertEquals(1, preferences.subscriptionCount.value)

            // A configuration change, such as a new language, takes less than this, so the reads survive it.
            screen.cancel()
            advanceTimeBy(4_999)
            runCurrent()
            assertEquals(1, preferences.subscriptionCount.value)

            advanceTimeBy(2)
            runCurrent()
            assertEquals(0, preferences.subscriptionCount.value)
        }

    /** The next state after Loading. */
    private suspend fun ReceiveTurbine<SettingsUiState>.awaitSettled(): SettingsUiState {
        var state = awaitItem()
        while (state == SettingsUiState.Loading) state = awaitItem()
        return state
    }
}

/** Android's language setting in memory. */
internal class FakeAppLanguages(
    initial: AppLanguage = AppLanguage.SYSTEM,
) : AppLanguages {
    override val current = MutableStateFlow(initial)

    override fun set(language: AppLanguage) {
        current.value = language
    }
}
