package com.ruizurraca.logrobus.feature.settings.domain

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.preferences.EtaFormat
import com.ruizurraca.logrobus.core.preferences.PreferencesStore
import com.ruizurraca.logrobus.core.preferences.ThemePreference
import com.ruizurraca.logrobus.core.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * FR-SET-01: the user's settings now, then every change. An interface, because Swift calls it and its tests replace
 * it.
 */
fun interface ObservePreferences {
    operator fun invoke(): Flow<UserPreferences>
}

class DefaultObservePreferences(
    private val store: PreferencesStore,
) : ObservePreferences {
    override fun invoke(): Flow<UserPreferences> = store.preferences
}

/** FR-SET-01: the app's theme, kept for the next launch. */
fun interface SetTheme {
    suspend operator fun invoke(theme: ThemePreference): AppResult<Unit>
}

class DefaultSetTheme(
    private val store: PreferencesStore,
) : SetTheme {
    override suspend fun invoke(theme: ThemePreference): AppResult<Unit> = store.setTheme(theme)
}

/** FR-SET-01: the ETA format, minutes or clock time, kept for the next launch. */
fun interface SetEtaFormat {
    suspend operator fun invoke(format: EtaFormat): AppResult<Unit>
}

class DefaultSetEtaFormat(
    private val store: PreferencesStore,
) : SetEtaFormat {
    override suspend fun invoke(format: EtaFormat): AppResult<Unit> = store.setEtaFormat(format)
}
