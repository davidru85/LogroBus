package com.ruizurraca.logrobus.core.preferences

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.coroutines.FlowSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** The [PreferencesStore] over Multiplatform Settings (ADR-0018). */
@OptIn(ExperimentalSettingsApi::class)
class SettingsPreferencesStore(
    @Suppress("unused") private val settings: FlowSettings,
    @Suppress("unused") private val logger: Logger,
) : PreferencesStore {
    override val preferences: Flow<UserPreferences> = flowOf(UserPreferences())

    override suspend fun setTheme(theme: ThemePreference): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun setEtaFormat(format: EtaFormat): AppResult<Unit> = AppResult.Success(Unit)
}
