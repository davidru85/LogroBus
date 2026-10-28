package com.ruizurraca.logrobus.android.feature.settings

import app.cash.turbine.test
import com.ruizurraca.logrobus.feature.settings.domain.DatasetSync
import com.ruizurraca.logrobus.feature.settings.presentation.AboutDataUiMapper
import com.ruizurraca.logrobus.feature.settings.presentation.AboutDataUiState
import com.ruizurraca.logrobus.transit.domain.StaticDataset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.time.Instant

/** ADR-0007: an Android ViewModel builds its state only with the shared use case and mapper. */
@OptIn(ExperimentalCoroutinesApi::class)
class AboutDataViewModelTest {
    private val lastSyncs = MutableSharedFlow<List<DatasetSync>>()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun lb010_theViewModelLoads_thenShowsWhatTheSharedMapperMakesOfTheUseCase() =
        runTest {
            val viewModel = AboutDataViewModel { lastSyncs }
            val syncs =
                listOf(
                    DatasetSync(StaticDataset.LINES, Instant.parse("2026-10-28T09:30:00Z")),
                    DatasetSync(StaticDataset.STOPS, null),
                )

            viewModel.state.test {
                assertEquals(AboutDataUiState.Loading, awaitItem())
                lastSyncs.emit(syncs)
                assertEquals(AboutDataUiMapper.content(syncs), awaitItem())
            }
        }
}
