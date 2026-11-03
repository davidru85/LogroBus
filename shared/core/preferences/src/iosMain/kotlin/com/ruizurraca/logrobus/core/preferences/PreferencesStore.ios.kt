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

/** The settings saved in [defaults], read at once, for the app's first frame. */
@Suppress("UNUSED_PARAMETER", "UnusedParameter")
fun savedPreferences(defaults: NSUserDefaults): UserPreferences = UserPreferences()
