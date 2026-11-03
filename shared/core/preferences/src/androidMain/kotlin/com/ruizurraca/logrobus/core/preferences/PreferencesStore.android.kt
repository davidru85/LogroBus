package com.ruizurraca.logrobus.core.preferences

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.warn
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.datastore.DataStoreSettings
import java.io.File

/** The user's settings on Android, in DataStore in the app's files directory (ADR-0018). One per process. */
fun dataStorePreferencesStore(
    context: Context,
    logger: Logger,
): PreferencesStore = dataStorePreferencesStore(File(context.filesDir, "datastore/$FILE_NAME"), logger)

@OptIn(ExperimentalSettingsApi::class, ExperimentalSettingsImplementation::class)
internal fun dataStorePreferencesStore(
    file: File,
    logger: Logger,
): PreferencesStore {
    val dataStore =
        PreferenceDataStoreFactory.create(
            // A damaged file starts again from the defaults, rather than failing every read and write after it.
            corruptionHandler =
                ReplaceFileCorruptionHandler { cause ->
                    logger.warn(TAG, "The settings file was damaged, so it starts again", cause)
                    emptyPreferences()
                },
            produceFile = { file },
        )
    return SettingsPreferencesStore(DataStoreSettings(dataStore), logger)
}

// DataStore's preferences files must end in .preferences_pb.
private const val FILE_NAME = "settings.preferences_pb"
private const val TAG = "Preferences"
