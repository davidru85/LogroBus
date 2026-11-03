package com.ruizurraca.logrobus.android.app

import androidx.appcompat.app.AppCompatDelegate
import com.ruizurraca.logrobus.core.preferences.ThemePreference

/**
 * FR-SET-01: the user's theme as AppCompat's night mode (ADR-0020), which the window, the system bars and Compose's
 * isSystemInDarkTheme() all follow.
 */
internal fun nightModeOf(theme: ThemePreference): Int =
    when (theme) {
        ThemePreference.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        ThemePreference.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        ThemePreference.DARK -> AppCompatDelegate.MODE_NIGHT_YES
    }
