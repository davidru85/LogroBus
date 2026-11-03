package com.ruizurraca.logrobus.core.preferences

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.warn
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.coroutines.FlowSettings
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * The [PreferencesStore] over Multiplatform Settings (ADR-0018): each setting is its enum's name under its own key. A
 * value this version does not know, from a later version say, reads as the default.
 */
@OptIn(ExperimentalSettingsApi::class)
class SettingsPreferencesStore(
    private val settings: FlowSettings,
    private val logger: Logger,
) : PreferencesStore {
    override val preferences: Flow<UserPreferences> =
        combine(settings.getStringOrNullFlow(THEME), settings.getStringOrNullFlow(ETA_FORMAT)) { theme, format ->
            UserPreferences(
                theme = ThemePreference.entries.firstOrNull { it.name == theme } ?: ThemePreference.SYSTEM,
                etaFormat = EtaFormat.entries.firstOrNull { it.name == format } ?: EtaFormat.MINUTES,
            )
        }.distinctUntilChanged()
            // The storage boundary (code_style.md §2.2): settings that cannot be read are the defaults, never a crash.
            .catch { failure ->
                logger.warn(TAG, "Reading the settings failed", failure)
                emit(UserPreferences())
            }

    override suspend fun setTheme(theme: ThemePreference): AppResult<Unit> = put(THEME, theme.name)

    override suspend fun setEtaFormat(format: EtaFormat): AppResult<Unit> = put(ETA_FORMAT, format.name)

    /** The storage boundary: a write that fails fails this change alone. Only the caller's cancellation propagates. */
    @Suppress("TooGenericExceptionCaught") // On purpose: see above.
    private suspend fun put(
        key: String,
        value: String,
    ): AppResult<Unit> =
        try {
            AppResult.Success(settings.putString(key, value))
        } catch (cause: Exception) {
            currentCoroutineContext().ensureActive()
            logger.warn(TAG, "Saving a setting failed", cause)
            AppResult.Failure(AppError.Data)
        }

    private companion object {
        const val TAG = "Preferences"
        const val THEME = "theme"
        const val ETA_FORMAT = "eta_format"
    }
}
