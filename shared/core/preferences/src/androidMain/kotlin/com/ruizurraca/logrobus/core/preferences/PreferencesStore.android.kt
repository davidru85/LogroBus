package com.ruizurraca.logrobus.core.preferences

import android.content.Context
import com.ruizurraca.logrobus.core.common.Logger
import java.io.File

/** The user's settings on Android, in DataStore in the app's files directory (ADR-0018). One per process. */
fun dataStorePreferencesStore(
    context: Context,
    logger: Logger,
): PreferencesStore = dataStorePreferencesStore(File(context.filesDir, "datastore/$FILE_NAME"), logger)

@Suppress("UNUSED_PARAMETER", "UnusedParameter")
internal fun dataStorePreferencesStore(
    file: File,
    logger: Logger,
): PreferencesStore = TODO("LB-403")

// DataStore's preferences files must end in .preferences_pb.
private const val FILE_NAME = "settings.preferences_pb"
