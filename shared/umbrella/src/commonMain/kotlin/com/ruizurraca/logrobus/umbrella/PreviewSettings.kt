package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.preferences.EtaFormat
import com.ruizurraca.logrobus.core.preferences.PreferencesStore
import com.ruizurraca.logrobus.core.preferences.ThemePreference
import com.ruizurraca.logrobus.core.preferences.UserPreferences
import com.ruizurraca.logrobus.feature.settings.domain.AppCaches
import com.ruizurraca.logrobus.feature.settings.domain.ClearCache
import com.ruizurraca.logrobus.feature.settings.domain.DefaultClearCache
import com.ruizurraca.logrobus.feature.settings.domain.DefaultObservePreferences
import com.ruizurraca.logrobus.feature.settings.domain.DefaultSetEtaFormat
import com.ruizurraca.logrobus.feature.settings.domain.DefaultSetTheme
import com.ruizurraca.logrobus.feature.settings.domain.ObservePreferences
import com.ruizurraca.logrobus.feature.settings.domain.SetEtaFormat
import com.ruizurraca.logrobus.feature.settings.domain.SetTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.concurrent.Volatile

/**
 * Settings' use cases (LB-403) over a store and caches in memory, like the device's: a setting changed in a preview or
 * test shows at once. [PreviewUseCases] scripts them.
 */
internal class PreviewSettings : SettingsUseCases {
    private val preferences = MutableStateFlow(UserPreferences())
    private val clears = MutableStateFlow(0)

    /** A device whose storage cannot be written: saving a setting and clearing the cache fail with a data error. */
    @Volatile var failing = false

    val cacheClears: Int
        get() = clears.value

    fun script(
        theme: ThemePreference,
        etaFormat: EtaFormat,
    ) {
        preferences.value = UserPreferences(theme, etaFormat)
    }

    private val store =
        object : PreferencesStore {
            override val preferences = this@PreviewSettings.preferences

            override suspend fun setTheme(theme: ThemePreference) = save { it.copy(theme = theme) }

            override suspend fun setEtaFormat(format: EtaFormat) = save { it.copy(etaFormat = format) }

            private fun save(change: (UserPreferences) -> UserPreferences): AppResult<Unit> {
                if (failing) return AppResult.Failure(AppError.Data)
                preferences.update(change)
                return AppResult.Success(Unit)
            }
        }

    private val caches =
        object : AppCaches {
            override suspend fun clear(): AppResult<Unit> {
                if (failing) return AppResult.Failure(AppError.Data)
                clears.update { it + 1 }
                return AppResult.Success(Unit)
            }
        }

    override fun observePreferences(): ObservePreferences = DefaultObservePreferences(store)

    override fun setTheme(): SetTheme = DefaultSetTheme(store)

    override fun setEtaFormat(): SetEtaFormat = DefaultSetEtaFormat(store)

    override fun clearCache(): ClearCache = DefaultClearCache(caches)
}
