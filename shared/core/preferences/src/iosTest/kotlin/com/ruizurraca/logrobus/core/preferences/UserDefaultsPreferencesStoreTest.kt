package com.ruizurraca.logrobus.core.preferences

import app.cash.turbine.test
import com.ruizurraca.logrobus.core.common.LogLevel
import com.ruizurraca.logrobus.core.common.Logger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSUserDefaults
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** ADR-0018 on iOS: the settings live in the app's user defaults. */
class UserDefaultsPreferencesStoreTest {
    private val suite = "com.ruizurraca.logrobus.tests.preferences"
    private val defaults = checkNotNull(NSUserDefaults(suiteName = suite))
    private val logger =
        object : Logger {
            override fun log(
                level: LogLevel,
                tag: String,
                message: String,
                throwable: Throwable?,
            ) = Unit
        }

    @AfterTest
    fun forget() = defaults.removePersistentDomainForName(suite)

    @Test
    fun lb403_userDefaultsKeepTheSettings_forTheNextLaunch() =
        runTest {
            userDefaultsPreferencesStore(
                logger,
                defaults,
                StandardTestDispatcher(testScheduler),
            ).setTheme(ThemePreference.LIGHT)

            assertEquals(
                UserPreferences(theme = ThemePreference.LIGHT),
                userDefaultsPreferencesStore(
                    logger,
                    defaults,
                    StandardTestDispatcher(testScheduler),
                ).preferences.first(),
            )
        }

    @Test
    fun lb403_theSavedSettingsCanBeReadAtOnce_forTheAppsFirstFrame() =
        runTest {
            userDefaultsPreferencesStore(logger, defaults, StandardTestDispatcher(testScheduler))
                .setTheme(ThemePreference.DARK)

            assertEquals(UserPreferences(theme = ThemePreference.DARK), savedPreferences(defaults))
        }

    @Test
    fun lb403_userDefaultsSayWhenTheSettingsChange() =
        runTest {
            val store = userDefaultsPreferencesStore(logger, defaults, StandardTestDispatcher(testScheduler))

            store.preferences.test {
                assertEquals(UserPreferences(), awaitItem())

                store.setEtaFormat(EtaFormat.CLOCK_TIME)
                assertEquals(UserPreferences(etaFormat = EtaFormat.CLOCK_TIME), awaitItem())
            }
        }
}
