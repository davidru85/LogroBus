package com.ruizurraca.logrobus.core.preferences

import com.ruizurraca.logrobus.core.common.AppResult
import kotlinx.coroutines.flow.Flow

/** FR-SET-01: the app's appearance. [SYSTEM] follows the device's light or dark mode. */
enum class ThemePreference { SYSTEM, LIGHT, DARK }

/** FR-SET-01: how ETAs show, as minutes (spec §5.7) or always as a clock time. */
enum class EtaFormat { MINUTES, CLOCK_TIME }

/** The user's settings (FR-SET-01). The app's language is the platform's own setting, so it is not here (ADR-0020). */
data class UserPreferences(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val etaFormat: EtaFormat = EtaFormat.MINUTES,
)

/**
 * The user's settings on the device (ADR-0018). They are kept across launches, and every change reaches the
 * observers of [preferences].
 */
interface PreferencesStore {
    /** The settings now, then every change. A store that cannot be read gives the defaults. */
    val preferences: Flow<UserPreferences>

    suspend fun setTheme(theme: ThemePreference): AppResult<Unit>

    suspend fun setEtaFormat(format: EtaFormat): AppResult<Unit>
}
