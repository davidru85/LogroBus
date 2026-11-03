package com.ruizurraca.logrobus.feature.settings.domain

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.preferences.EtaFormat
import com.ruizurraca.logrobus.core.preferences.ThemePreference
import com.ruizurraca.logrobus.core.preferences.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** FR-SET-01: Settings reads and changes the theme and the ETA format through the store. */
class PreferencesTest {
    private val store = FakePreferencesStore(UserPreferences(ThemePreference.DARK, EtaFormat.MINUTES))

    @Test
    fun lb403_settingsShowTheStoredPreferences() =
        runTest {
            assertEquals(
                UserPreferences(ThemePreference.DARK, EtaFormat.MINUTES),
                DefaultObservePreferences(store)().first(),
            )
        }

    @Test
    fun lb403_aNewThemeAndEtaFormat_areStored() =
        runTest {
            assertEquals(AppResult.Success(Unit), DefaultSetTheme(store)(ThemePreference.LIGHT))
            assertEquals(AppResult.Success(Unit), DefaultSetEtaFormat(store)(EtaFormat.CLOCK_TIME))

            assertEquals(UserPreferences(ThemePreference.LIGHT, EtaFormat.CLOCK_TIME), store.preferences.value)
        }

    @Test
    fun lb403_aChangeTheStoreCannotSave_isAFailure() =
        runTest {
            store.failing = true

            assertEquals(AppResult.Failure(AppError.Data), DefaultSetTheme(store)(ThemePreference.LIGHT))
            assertEquals(AppResult.Failure(AppError.Data), DefaultSetEtaFormat(store)(EtaFormat.CLOCK_TIME))
        }
}
