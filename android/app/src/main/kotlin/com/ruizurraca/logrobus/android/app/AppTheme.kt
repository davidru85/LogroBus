package com.ruizurraca.logrobus.android.app

import androidx.appcompat.app.AppCompatDelegate
import com.ruizurraca.logrobus.core.preferences.ThemePreference

/** FR-SET-01: the user's theme as AppCompat's night mode (ADR-0020). */
@Suppress("UNUSED_PARAMETER", "UnusedParameter")
internal fun nightModeOf(theme: ThemePreference): Int = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
