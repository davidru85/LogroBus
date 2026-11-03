package com.ruizurraca.logrobus.core.preferences

import com.ruizurraca.logrobus.core.common.Logger
import platform.Foundation.NSUserDefaults

/** The user's settings on iOS, in the app's [defaults] (ADR-0018). */
@Suppress("UNUSED_PARAMETER", "UnusedParameter")
fun userDefaultsPreferencesStore(
    logger: Logger,
    defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
): PreferencesStore = TODO("LB-403")
