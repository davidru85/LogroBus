package com.ruizurraca.logrobus.core.preferences

import com.ruizurraca.logrobus.core.common.Logger
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import kotlinx.coroutines.CoroutineDispatcher
import platform.Foundation.NSUserDefaults

/** The user's settings on iOS, in the app's [defaults] (ADR-0018), read and written on [io]. */
@OptIn(ExperimentalSettingsApi::class)
fun userDefaultsPreferencesStore(
    logger: Logger,
    defaults: NSUserDefaults,
    io: CoroutineDispatcher,
): PreferencesStore = SettingsPreferencesStore(NSUserDefaultsSettings(defaults).toFlowSettings(io), logger)

/**
 * The settings saved in [defaults], read at once: the app's first frame takes its theme from them, rather than wait
 * for the store's first value (ADR-0020). User defaults are read synchronously, so nothing blocks on a thread.
 */
fun savedPreferences(defaults: NSUserDefaults): UserPreferences {
    val saved = NSUserDefaultsSettings(defaults)
    return preferencesOf(saved.getStringOrNull(PreferenceKeys.THEME), saved.getStringOrNull(PreferenceKeys.ETA_FORMAT))
}
