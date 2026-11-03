package com.ruizurraca.logrobus.core.preferences

import app.cash.turbine.test
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.LogLevel
import com.ruizurraca.logrobus.core.common.Logger
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalSettingsApi::class)
class PreferencesStoreTest {
    private val saved = MapSettings()
    private val logger = RecordingLogger()

    private fun store(settings: FlowSettings = saved.toFlowSettings(UnconfinedTestDispatcher())) =
        SettingsPreferencesStore(settings, logger)

    @Test
    fun lb403_aFirstLaunch_followsTheSystemThemeAndShowsMinutes() =
        runTest {
            assertEquals(
                UserPreferences(ThemePreference.SYSTEM, EtaFormat.MINUTES),
                store().preferences.first(),
            )
        }

    @Test
    fun lb403_aChosenThemeAndEtaFormat_areKeptForTheNextLaunch() =
        runTest {
            store().setTheme(ThemePreference.DARK)
            store().setEtaFormat(EtaFormat.CLOCK_TIME)

            assertEquals(
                UserPreferences(ThemePreference.DARK, EtaFormat.CLOCK_TIME),
                store().preferences.first(),
            )
        }

    @Test
    fun lb403_aChange_reachesTheScreensThatShowTheSettings() =
        runTest {
            val store = store()
            store.preferences.test {
                assertEquals(UserPreferences(), awaitItem())

                assertEquals(AppResult.Success(Unit), store.setTheme(ThemePreference.LIGHT))
                assertEquals(UserPreferences(theme = ThemePreference.LIGHT), awaitItem())

                assertEquals(AppResult.Success(Unit), store.setEtaFormat(EtaFormat.CLOCK_TIME))
                assertEquals(UserPreferences(ThemePreference.LIGHT, EtaFormat.CLOCK_TIME), awaitItem())
            }
        }

    @Test
    fun lb403_aValueThisVersionDoesNotKnow_readsAsTheDefault() =
        runTest {
            saved.putString("theme", "SEPIA")
            saved.putString("eta_format", "")

            assertEquals(UserPreferences(), store().preferences.first())
        }

    @Test
    fun lb403_aStoreThatCannotBeRead_givesTheDefaults() =
        runTest {
            val unreadable = BrokenSettings(saved.toFlowSettings(UnconfinedTestDispatcher()))

            assertEquals(UserPreferences(), store(unreadable).preferences.first())
            assertTrue(logger.warnings.isNotEmpty())
        }

    @Test
    fun lb403_aWriteThatFails_isAFailureOfThatChangeAlone() =
        runTest {
            val unwritable = BrokenSettings(saved.toFlowSettings(UnconfinedTestDispatcher()))

            assertEquals(AppResult.Failure(AppError.Data), store(unwritable).setTheme(ThemePreference.DARK))
            assertEquals(AppResult.Failure(AppError.Data), store(unwritable).setEtaFormat(EtaFormat.CLOCK_TIME))
            assertEquals(2, logger.warnings.size)
        }
}

/** Settings whose strings can be neither read nor written, as a damaged file would be. */
@OptIn(ExperimentalSettingsApi::class)
private class BrokenSettings(
    delegate: FlowSettings,
) : FlowSettings by delegate {
    override fun getStringOrNullFlow(key: String): Flow<String?> = flow { throw IllegalStateException("damaged") }

    override suspend fun putString(
        key: String,
        value: String,
    ): Unit = throw IllegalStateException("damaged")
}

private class RecordingLogger : Logger {
    val warnings = mutableListOf<String>()

    override fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        if (level == LogLevel.Warn) warnings += message
    }
}
