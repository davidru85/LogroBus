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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalSettingsApi::class, ExperimentalCoroutinesApi::class)
class PreferencesStoreTest {
    private val saved = MapSettings()
    private val logger = RecordingLogger()

    private fun TestScope.store(
        settings: FlowSettings = saved.toFlowSettings(UnconfinedTestDispatcher(testScheduler)),
    ) = SettingsPreferencesStore(settings, logger)

    private fun TestScope.settings() = saved.toFlowSettings(UnconfinedTestDispatcher(testScheduler))

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
            saved.putString("settings.theme", "SEPIA")
            saved.putString("settings.eta_format", "")

            assertEquals(UserPreferences(), store().preferences.first())
        }

    @Test
    fun lb403_aStoreThatCannotBeRead_givesTheDefaults() =
        runTest {
            val unreadable = BrokenSettings(settings())

            assertEquals(UserPreferences(), store(unreadable).preferences.first())
            assertTrue(logger.warnings.isNotEmpty())
        }

    @Test
    fun lb403_aWriteThatFails_isAFailureOfThatChangeAlone() =
        runTest {
            val unwritable = BrokenSettings(settings())

            assertEquals(AppResult.Failure(AppError.Data), store(unwritable).setTheme(ThemePreference.DARK))
            assertEquals(AppResult.Failure(AppError.Data), store(unwritable).setEtaFormat(EtaFormat.CLOCK_TIME))
            assertEquals(2, logger.warnings.size)
        }

    @Test
    fun lb403_aReadThatFailsOnce_givesTheDefaults_thenFollowsTheSettingsAgain() =
        runTest {
            // A read that fails must not end the flow: the theme chosen next still reaches the app.
            val flaky = FlakySettings(settings())
            val store = store(flaky)

            store.preferences.test {
                assertEquals(UserPreferences(), awaitItem())

                store.setTheme(ThemePreference.DARK)
                assertEquals(UserPreferences(theme = ThemePreference.DARK), awaitItem())
            }
            assertEquals(1, logger.warnings.size)
        }

    @Test
    fun lb403_theSettingsAreKeptUnderKeysOfTheirOwn() =
        runTest {
            // On iOS they share the app's user defaults with every framework, so the keys say whose they are.
            store().setTheme(ThemePreference.DARK)
            store().setEtaFormat(EtaFormat.CLOCK_TIME)

            assertEquals("DARK", saved.getStringOrNull("settings.theme"))
            assertEquals("CLOCK_TIME", saved.getStringOrNull("settings.eta_format"))
            assertNull(saved.getStringOrNull("theme"))
        }

    @Test
    fun lb403_aSaveThatIsCancelled_isCancelled_notAFailure() =
        runTest {
            val save = launch { store(HangingSettings(settings())).setTheme(ThemePreference.DARK) }
            testScheduler.runCurrent()

            save.cancel()
            save.join()

            assertTrue(save.isCancelled)
            assertEquals(emptyList(), logger.warnings)
        }
}

/** Settings whose first read fails, as a file busy at launch might, and then work. */
@OptIn(ExperimentalSettingsApi::class)
private class FlakySettings(
    private val delegate: FlowSettings,
) : FlowSettings by delegate {
    private var reads = 0

    override fun getStringOrNullFlow(key: String): Flow<String?> =
        flow {
            if (reads++ == 0) error("busy")
            emitAll(delegate.getStringOrNullFlow(key))
        }
}

/** Settings whose writes never finish. */
@OptIn(ExperimentalSettingsApi::class)
private class HangingSettings(
    delegate: FlowSettings,
) : FlowSettings by delegate {
    override suspend fun putString(
        key: String,
        value: String,
    ): Unit = awaitCancellation()
}

/** Settings whose strings can be neither read nor written, as a damaged file would be. */
@OptIn(ExperimentalSettingsApi::class)
private class BrokenSettings(
    delegate: FlowSettings,
) : FlowSettings by delegate {
    override fun getStringOrNullFlow(key: String): Flow<String?> = flow { error("damaged") }

    override suspend fun putString(
        key: String,
        value: String,
    ): Unit = error("damaged")
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
