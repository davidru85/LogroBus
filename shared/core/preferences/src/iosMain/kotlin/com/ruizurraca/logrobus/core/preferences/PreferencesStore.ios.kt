package com.ruizurraca.logrobus.core.preferences

import com.ruizurraca.logrobus.core.common.Logger
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import platform.Foundation.NSUserDefaults

/** The user's settings on iOS, in the app's [defaults] (ADR-0018). */
@OptIn(ExperimentalSettingsApi::class)
fun userDefaultsPreferencesStore(
    logger: Logger,
    defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
): PreferencesStore = SettingsPreferencesStore(NSUserDefaultsSettings(defaults).toFlowSettings(), logger)
