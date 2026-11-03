package com.ruizurraca.logrobus.feature.settings.presentation

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.preferences.EtaFormat
import com.ruizurraca.logrobus.core.preferences.ThemePreference
import com.ruizurraca.logrobus.core.preferences.UserPreferences
import com.ruizurraca.logrobus.core.presentation.MadridDateTime
import com.ruizurraca.logrobus.feature.settings.domain.DatasetSync

/**
 * The app's language (FR-SET-01). The platform keeps it, not the app's settings (ADR-0020): Android offers the three,
 * and iOS shows the one in use, which the system's Settings changes.
 */
enum class AppLanguage { SYSTEM, SPANISH, ENGLISH }

/** FR-SET-01: how fresh the static data is, by the last successful sync of lines and stops. */
sealed interface DataStatusUi {
    /** Every dataset has synced; [at] is the oldest of their last successful syncs. */
    data class Synced(
        val at: MadridDateTime,
    ) : DataStatusUi

    /** A dataset has never synced, so the data is not complete yet. */
    data object NeverSynced : DataStatusUi

    /** The sync status could not be read. */
    data object Unknown : DataStatusUi
}

/** Settings (LB-403, FR-SET-01). */
sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Content(
        val language: AppLanguage,
        val theme: ThemePreference,
        val etaFormat: EtaFormat,
        val dataStatus: DataStatusUi,
    ) : SettingsUiState
}

/** What a "Clear cache" leaves to say: done, or it could not. */
enum class CacheMessage { CLEARED, FAILED }

/** What Settings' screen sends its ViewModel. */
sealed interface SettingsEvent {
    /** Android: the app's language (ADR-0020). iOS opens the system's Settings instead. */
    data class SetLanguage(
        val language: AppLanguage,
    ) : SettingsEvent

    data class SetTheme(
        val theme: ThemePreference,
    ) : SettingsEvent

    data class SetEtaFormat(
        val format: EtaFormat,
    ) : SettingsEvent

    data object ClearCache : SettingsEvent

    /** The screen has shown the [CacheMessage], so it is not shown again. */
    data object CacheMessageShown : SettingsEvent
}

object SettingsUiMapper {
    /**
     * Settings with the [language] the platform reports, the stored [preferences] and the data status of [syncs]. A
     * sync status that cannot be read leaves the rest of Settings as it is.
     */
    fun state(
        preferences: UserPreferences,
        syncs: AppResult<List<DatasetSync>>,
        language: AppLanguage,
    ): SettingsUiState =
        SettingsUiState.Content(language, preferences.theme, preferences.etaFormat, dataStatusOf(syncs))

    /** What a "Clear cache" that ended with [result] leaves to say. */
    @Suppress("UNUSED_PARAMETER", "UnusedParameter")
    fun cacheMessage(result: AppResult<Unit>): CacheMessage = CacheMessage.CLEARED

    private fun dataStatusOf(syncs: AppResult<List<DatasetSync>>): DataStatusUi =
        when (syncs) {
            is AppResult.Success -> {
                val lastSuccesses = syncs.value.map { it.lastSuccess }
                val oldest = lastSuccesses.filterNotNull().minOrNull()
                if (oldest == null || null in lastSuccesses) {
                    DataStatusUi.NeverSynced
                } else {
                    DataStatusUi.Synced(MadridDateTime.of(oldest))
                }
            }

            is AppResult.Failure -> {
                DataStatusUi.Unknown
            }
        }
}
