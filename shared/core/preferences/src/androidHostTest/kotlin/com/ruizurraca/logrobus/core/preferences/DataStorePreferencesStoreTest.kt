package com.ruizurraca.logrobus.core.preferences

import app.cash.turbine.test
import com.ruizurraca.logrobus.core.common.LogLevel
import com.ruizurraca.logrobus.core.common.Logger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

/** ADR-0018 on Android: the settings live in DataStore. */
class DataStorePreferencesStoreTest {
    private val file = File(Files.createTempDirectory("datastore").toFile(), "settings.preferences_pb")
    private val logger =
        object : Logger {
            override fun log(
                level: LogLevel,
                tag: String,
                message: String,
                throwable: Throwable?,
            ) = Unit
        }

    @Test
    fun lb403_dataStoreKeepsTheSettings_andSaysWhenTheyChange() =
        runTest {
            val store = dataStorePreferencesStore(file, logger)

            store.preferences.test {
                assertEquals(UserPreferences(), awaitItem())

                store.setTheme(ThemePreference.DARK)
                assertEquals(UserPreferences(theme = ThemePreference.DARK), awaitItem())
            }
            assertEquals(true, file.length() > 0)
        }

    @Test
    fun lb403_aDamagedSettingsFile_startsAgainFromTheDefaults_andCanBeSavedTo() =
        runTest {
            file.parentFile.mkdirs()
            file.writeText("not a preferences file")
            val store = dataStorePreferencesStore(file, logger)

            assertEquals(UserPreferences(), store.preferences.first())

            store.setEtaFormat(EtaFormat.CLOCK_TIME)
            assertEquals(UserPreferences(etaFormat = EtaFormat.CLOCK_TIME), store.preferences.first())
        }
}
