package com.ruizurraca.logrobus.feature.settings.domain

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.preferences.EtaFormat
import com.ruizurraca.logrobus.core.preferences.PreferencesStore
import com.ruizurraca.logrobus.core.preferences.ThemePreference
import com.ruizurraca.logrobus.core.preferences.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** A store in memory, which can be told to fail its writes. */
internal class FakePreferencesStore(
    initial: UserPreferences = UserPreferences(),
) : PreferencesStore {
    var failing = false
    override val preferences = MutableStateFlow(initial)

    override suspend fun setTheme(theme: ThemePreference): AppResult<Unit> = write { it.copy(theme = theme) }

    override suspend fun setEtaFormat(format: EtaFormat): AppResult<Unit> = write { it.copy(etaFormat = format) }

    private fun write(change: (UserPreferences) -> UserPreferences): AppResult<Unit> {
        if (failing) return AppResult.Failure(AppError.Data)
        preferences.update(change)
        return AppResult.Success(Unit)
    }
}

/** Caches that count how often they were cleared, and can be told to fail. */
internal class FakeAppCaches : AppCaches {
    var failing = false
    var clears = 0

    override suspend fun clear(): AppResult<Unit> {
        if (failing) return AppResult.Failure(AppError.Data)
        clears++
        return AppResult.Success(Unit)
    }
}
