package com.ruizurraca.logrobus.feature.settings.presentation

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.preferences.EtaFormat
import com.ruizurraca.logrobus.core.preferences.ThemePreference
import com.ruizurraca.logrobus.core.preferences.UserPreferences
import com.ruizurraca.logrobus.core.presentation.MadridDateTime
import com.ruizurraca.logrobus.feature.settings.domain.DatasetSync
import com.ruizurraca.logrobus.transit.domain.StaticDataset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** FR-SET-01: Settings shows the language, the theme, the ETA format and how fresh the data is. */
class SettingsUiMapperTest {
    private val synced =
        AppResult.Success(
            listOf(
                DatasetSync(StaticDataset.LINES, Instant.parse("2026-11-03T09:30:00Z")),
                DatasetSync(StaticDataset.STOPS, Instant.parse("2026-11-03T08:15:00Z")),
            ),
        )

    private fun dataStatus(syncs: AppResult<List<DatasetSync>>) =
        (SettingsUiMapper.state(UserPreferences(), syncs, AppLanguage.SYSTEM) as SettingsUiState.Content).dataStatus

    @Test
    fun lb403_settingsOfferTheLanguageThemeAndEtaFormatInUse() {
        assertEquals(
            SettingsUiState.Content(
                AppLanguage.ENGLISH,
                ThemePreference.DARK,
                EtaFormat.CLOCK_TIME,
                DataStatusUi.Synced(MadridDateTime(2026, 11, 3, 9, 15)),
            ),
            SettingsUiMapper.state(
                UserPreferences(ThemePreference.DARK, EtaFormat.CLOCK_TIME),
                synced,
                AppLanguage.ENGLISH,
            ),
        )
    }

    @Test
    fun lb403_theDataStatus_isTheOldestLastSyncOnTheMadridClock() {
        // Stops synced at 08:15 UTC, 09:15 in Madrid: the data is as fresh as its oldest dataset.
        assertEquals(DataStatusUi.Synced(MadridDateTime(2026, 11, 3, 9, 15)), dataStatus(synced))
    }

    @Test
    fun lb403_aDatasetThatNeverSynced_meansTheDataIsNotThereYet() {
        val partly =
            AppResult.Success(
                listOf(
                    DatasetSync(StaticDataset.LINES, Instant.parse("2026-11-03T09:30:00Z")),
                    DatasetSync(StaticDataset.STOPS, null),
                ),
            )

        assertEquals(DataStatusUi.NeverSynced, dataStatus(partly))
        assertEquals(DataStatusUi.NeverSynced, dataStatus(AppResult.Success(emptyList())))
    }

    @Test
    fun lb403_aSyncStatusThatCannotBeRead_isUnknown_andTheSettingsStillShow() {
        assertEquals(
            SettingsUiState.Content(
                AppLanguage.SPANISH,
                ThemePreference.LIGHT,
                EtaFormat.MINUTES,
                DataStatusUi.Unknown,
            ),
            SettingsUiMapper.state(
                UserPreferences(theme = ThemePreference.LIGHT),
                AppResult.Failure(AppError.Data),
                AppLanguage.SPANISH,
            ),
        )
    }
}
