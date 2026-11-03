package com.ruizurraca.logrobus.android.feature.settings

import com.ruizurraca.logrobus.core.preferences.EtaFormat
import com.ruizurraca.logrobus.core.preferences.ThemePreference
import com.ruizurraca.logrobus.core.presentation.MadridDateTime
import com.ruizurraca.logrobus.feature.settings.presentation.AppLanguage
import com.ruizurraca.logrobus.feature.settings.presentation.DataStatusUi
import com.ruizurraca.logrobus.feature.settings.presentation.SettingsUiState

/** Settings as previews and screenshots show it: the system's language and theme, clock times, data synced at 10:30. */
internal object SettingsPreviewData {
    val content =
        SettingsUiState.Content(
            AppLanguage.SYSTEM,
            ThemePreference.SYSTEM,
            EtaFormat.CLOCK_TIME,
            DataStatusUi.Synced(MadridDateTime(2026, 11, 3, 10, 30)),
        )

    /** The data has never synced. */
    val neverSynced = content.copy(dataStatus = DataStatusUi.NeverSynced)

    /** The sync status could not be read: Settings' partial state. */
    val unknown = content.copy(dataStatus = DataStatusUi.Unknown)
}
