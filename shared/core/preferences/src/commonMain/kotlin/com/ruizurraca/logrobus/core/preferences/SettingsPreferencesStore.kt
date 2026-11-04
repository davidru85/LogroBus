package com.ruizurraca.logrobus.core.preferences

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.warn
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.coroutines.FlowSettings
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.retryWhen
import kotlin.time.Duration.Companion.seconds

/**
 * The [PreferencesStore] over Multiplatform Settings (ADR-0018): each setting is its enum's name under a key of its
 * own (`PreferenceKeys`). A value this version does not know, from a later version say, reads as the default.
 */
@OptIn(ExperimentalSettingsApi::class)
internal class SettingsPreferencesStore(
    private val settings: FlowSettings,
    private val logger: Logger,
) : PreferencesStore {
    override val preferences: Flow<UserPreferences> =
        combine(
            settings.getStringOrNullFlow(PreferenceKeys.THEME),
            settings.getStringOrNullFlow(PreferenceKeys.ETA_FORMAT),
            ::preferencesOf,
        )
            // The storage boundary (code_style.md §2.2): settings that cannot be read are the defaults, never a crash,
            // and the store is read again, so a later change still arrives. After a few failures the flow ends on the
            // defaults.
            .retryWhen { cause, attempt ->
                logger.warn(TAG, "Reading the settings failed", cause)
                if (attempt == 0L) emit(UserPreferences())
                delay(RETRY_DELAY * (attempt.toInt() + 1))
                attempt < MAX_RETRIES
            }.catch { emit(UserPreferences()) }
            .distinctUntilChanged()

    override suspend fun setTheme(theme: ThemePreference): AppResult<Unit> = put(PreferenceKeys.THEME, theme.name)

    override suspend fun setEtaFormat(format: EtaFormat): AppResult<Unit> = put(PreferenceKeys.ETA_FORMAT, format.name)

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
        const val MAX_RETRIES = 3L
        val RETRY_DELAY = 1.seconds
    }
}

/**
 * The keys of the settings. On iOS they share the app's user defaults with every framework, so they say whose they
 * are; renaming one would lose what users saved.
 */
internal object PreferenceKeys {
    const val THEME = "settings.theme"
    const val ETA_FORMAT = "settings.eta_format"
}

/** The settings that the saved [theme] and [etaFormat] names make; a name this version does not know is the default. */
internal fun preferencesOf(
    theme: String?,
    etaFormat: String?,
): UserPreferences =
    UserPreferences(
        theme = ThemePreference.entries.firstOrNull { it.name == theme } ?: ThemePreference.SYSTEM,
        etaFormat = EtaFormat.entries.firstOrNull { it.name == etaFormat } ?: EtaFormat.MINUTES,
    )
