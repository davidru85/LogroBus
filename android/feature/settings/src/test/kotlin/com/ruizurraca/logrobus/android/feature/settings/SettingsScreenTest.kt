package com.ruizurraca.logrobus.android.feature.settings

import android.content.Context
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.logrobus.core.preferences.EtaFormat
import com.ruizurraca.logrobus.core.preferences.ThemePreference
import com.ruizurraca.logrobus.core.presentation.MadridDateTime
import com.ruizurraca.logrobus.feature.settings.presentation.AppLanguage
import com.ruizurraca.logrobus.feature.settings.presentation.CacheMessage
import com.ruizurraca.logrobus.feature.settings.presentation.DataStatusUi
import com.ruizurraca.logrobus.feature.settings.presentation.SettingsEvent
import com.ruizurraca.logrobus.feature.settings.presentation.SettingsUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** FR-SET-01 on Android: Settings offers the language, theme, ETA format, data status and Clear cache (LB-403). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-h1200dp")
class SettingsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val events = mutableListOf<SettingsEvent>()
    private val content =
        SettingsUiState.Content(
            AppLanguage.SYSTEM,
            ThemePreference.DARK,
            EtaFormat.CLOCK_TIME,
            DataStatusUi.Synced(MadridDateTime(2026, 11, 3, 10, 30)),
        )

    private fun show(
        state: SettingsUiState = content,
        cacheMessage: CacheMessage? = null,
    ) = compose.setContent {
        SettingsScreen(state, onEvent = { events += it }, onBack = {}, cacheMessage = cacheMessage)
    }

    private fun text(id: Int) = compose.onNodeWithText(context.getString(id))

    @Test
    fun lb403_settingsOfferTheLanguageThemeEtaFormatDataStatusAndClearCache() {
        show()

        listOf(R.string.settings_appearance, R.string.settings_arrivals, R.string.settings_data).forEach {
            compose.onNode(hasText(context.getString(it)) and isHeading()).assertExists()
        }
        listOf(
            R.string.settings_language,
            R.string.settings_language_system,
            R.string.settings_theme,
            R.string.settings_theme_system,
            R.string.settings_theme_light,
            R.string.settings_theme_dark,
            R.string.settings_eta_format,
            R.string.settings_eta_format_minutes,
            R.string.settings_eta_format_clock_time,
            R.string.settings_data_status,
            R.string.settings_clear_cache,
        ).forEach { text(it).performScrollTo().assertExists() }
    }

    @Test
    fun lb403_eachSettingsChoices_areNamedByAHeading() {
        // TalkBack says which setting a radio button belongs to, and the name does not look like a row to tap.
        show()

        listOf(R.string.settings_theme, R.string.settings_eta_format).forEach {
            compose.onNode(hasText(context.getString(it)) and isHeading()).performScrollTo().assertExists()
        }
    }

    @Test
    @Config(qualifiers = "en-rUS-w640dp-h360dp-land")
    fun lb403_theLanguageDialog_scrollsOnASmallScreenAtLargeText() {
        // A phone in landscape at 200 %: the three languages do not fit, and the last must still be reachable.
        // The dialog is a window of its own, whose density comes from the configuration.
        RuntimeEnvironment.setFontScale(2f)
        show()

        text(R.string.settings_language).performClick()
        text(R.string.settings_language_english).performScrollTo().performClick()

        assertEquals(listOf(SettingsEvent.SetLanguage(AppLanguage.ENGLISH)), events)
    }

    @Test
    fun lb403_theThemeAndEtaFormatInUse_areSelected() {
        show()

        text(R.string.settings_theme_dark).assertIsSelected()
        text(R.string.settings_theme_system).assertIsNotSelected()
        text(R.string.settings_eta_format_clock_time).assertIsSelected()
        text(R.string.settings_eta_format_minutes).assertIsNotSelected()
    }

    @Test
    fun lb403_choosingAThemeOrAnEtaFormat_sendsIt() {
        show()

        text(R.string.settings_theme_light).performClick()
        text(R.string.settings_eta_format_minutes).performScrollTo().performClick()

        assertEquals(
            listOf(SettingsEvent.SetTheme(ThemePreference.LIGHT), SettingsEvent.SetEtaFormat(EtaFormat.MINUTES)),
            events,
        )
    }

    @Test
    fun lb403_theLanguage_offersSystemSpanishAndEnglish_andSendsTheOneChosen() {
        show()

        text(R.string.settings_language).performClick()

        compose.onNode(isDialog()).assertExists()
        text(R.string.settings_language_spanish).assertExists()
        compose
            .onNode(
                hasText(context.getString(R.string.settings_language_system)) and isInDialog(),
            ).assertIsSelected()
        text(R.string.settings_language_english).performClick()

        assertEquals(listOf(SettingsEvent.SetLanguage(AppLanguage.ENGLISH)), events)
        compose.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun lb403_theLanguageDialog_cancelsWithoutAChange() {
        show()

        text(R.string.settings_language).performClick()
        text(R.string.settings_cancel).performClick()

        assertEquals(emptyList<SettingsEvent>(), events)
        compose.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun lb403_theDataStatus_saysWhenTheDataLastSynced() {
        show()

        compose.onNodeWithText("10:30", substring = true).performScrollTo().assertExists()
    }

    @Test
    fun lb403_theDataStatus_saysWhenTheDataHasNotSynced() {
        show(content.copy(dataStatus = DataStatusUi.NeverSynced))
        text(R.string.settings_data_status_never).performScrollTo().assertExists()
    }

    @Test
    fun lb403_aStatusThatCannotBeRead_saysSo() {
        show(content.copy(dataStatus = DataStatusUi.Unknown))
        text(R.string.settings_data_status_unknown).performScrollTo().assertExists()
    }

    @Test
    fun lb403_clearCache_sendsIt_andSaysWhatItKeeps() {
        show()

        text(R.string.settings_clear_cache_detail).performScrollTo().assertExists()
        text(R.string.settings_clear_cache).performClick()

        assertEquals(listOf(SettingsEvent.ClearCache), events)
    }

    @Test
    fun lb403_aClearedCache_isSaidOnce() {
        show(cacheMessage = CacheMessage.CLEARED)

        text(R.string.settings_cache_cleared).assertExists()
        compose.mainClock.advanceTimeBy(SNACKBAR_SHORT_MILLIS)
        compose.waitForIdle()

        assertEquals(listOf(SettingsEvent.CacheMessageShown), events)
    }

    @Test
    fun lb403_aCacheThatCouldNotBeCleared_isSaid() {
        show(cacheMessage = CacheMessage.FAILED)

        text(R.string.settings_cache_failed).assertExists()
    }

    @Test
    fun lb403_theFirstLoad_showsTheLayout_notASpinner() {
        show(SettingsUiState.Loading)

        text(R.string.settings_theme_light).assertExists().assertIsNotSelected()
        compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()
    }

    private fun isInDialog() = hasAnyAncestor(isDialog())

    private companion object {
        // A short snackbar's time on screen, with room to spare.
        const val SNACKBAR_SHORT_MILLIS = 10_000L
    }
}
