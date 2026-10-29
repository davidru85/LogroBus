package com.ruizurraca.logrobus.android.feature.settings

import android.content.Context
import android.provider.Settings
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.feature.settings.domain.DatasetSync
import com.ruizurraca.logrobus.feature.settings.presentation.AboutDataEvent
import com.ruizurraca.logrobus.feature.settings.presentation.AboutDataUiMapper
import com.ruizurraca.logrobus.feature.settings.presentation.AboutDataUiState
import com.ruizurraca.logrobus.transit.domain.StaticDataset
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.TimeZone
import kotlin.time.Instant

/** Spec §7.3 and FR-ERR-05 on Android: About data's states, and its times on the Madrid clock (LB-405). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AboutDataScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val synced =
        AboutDataUiMapper.state(
            AppResult.Success(
                listOf(
                    DatasetSync(StaticDataset.LINES, Instant.parse("2026-10-28T09:30:00Z")),
                    DatasetSync(StaticDataset.STOPS, null),
                ),
            ),
        )

    @Test
    fun lb010_theFirstLoadShowsTheLayoutWithPlaceholders_notASpinner() {
        compose.setContent { AboutDataScreen(AboutDataUiState.Loading, onEvent = {}, onBack = {}) }

        compose.onNodeWithText(context.getString(R.string.about_dataset_lines)).assertExists()
        compose.onNodeWithText(context.getString(R.string.about_dataset_stops)).assertExists()
        compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()
    }

    @Test
    fun lb010_anErrorShowsItsReason_andRetrySendsTheEvent() {
        val events = mutableListOf<AboutDataEvent>()
        compose.setContent {
            AboutDataScreen(AboutDataUiState.Error(ErrorReason.DATA), onEvent = { events += it }, onBack = {})
        }

        compose.onNodeWithText(context.getString(R.string.error_data)).assertExists()
        compose.onNodeWithText(context.getString(R.string.error_retry)).performClick()

        assertEquals(listOf(AboutDataEvent.Retry), events)
    }

    @Test
    @Config(qualifiers = "es-rES")
    fun lb405_syncTimesShowOnTheMadridClock_whateverTheDeviceZone() {
        val deviceZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        try {
            compose.setContent { AboutDataScreen(synced, onEvent = {}, onBack = {}) }

            // 09:30 UTC is 10:30 in Madrid (CET); New York would say 05:30.
            compose.onNodeWithText("10:30", substring = true).assertExists()
        } finally {
            TimeZone.setDefault(deviceZone)
        }
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun lb405_timesFollowTheDevicesTwentyFourHourSetting() {
        Settings.System.putString(context.contentResolver, Settings.System.TIME_12_24, "24")

        compose.setContent { AboutDataScreen(synced, onEvent = {}, onBack = {}) }

        compose.onNodeWithText("10:30", substring = true).assertExists()
        compose.onNodeWithText("AM", substring = true).assertDoesNotExist()
    }
}
