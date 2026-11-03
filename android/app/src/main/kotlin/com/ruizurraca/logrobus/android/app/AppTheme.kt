package com.ruizurraca.logrobus.android.app

import android.app.UiModeManager
import android.content.Context
import android.os.Build
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

/** The user's theme as the system's night mode for this app, from Android 12. */
@Suppress("UNUSED_PARAMETER", "UnusedParameter")
internal fun applicationNightModeOf(theme: ThemePreference): Int = UiModeManager.MODE_NIGHT_AUTO

/** Applies [theme] to the whole app on an Android of version [sdk]. */
@Suppress("UNUSED_PARAMETER", "UnusedParameter")
internal fun applyTheme(
    context: Context,
    theme: ThemePreference,
    sdk: Int = Build.VERSION.SDK_INT,
) {
    AppCompatDelegate.setDefaultNightMode(nightModeOf(theme))
}

/** Before the activity's first frame, on an Android of version [sdk]: the saved theme, which [read] gives. */
@Suppress("UNUSED_PARAMETER", "UnusedParameter")
internal fun applySavedThemeBeforeTheFirstFrame(
    sdk: Int = Build.VERSION.SDK_INT,
    read: () -> ThemePreference,
) {
    AppCompatDelegate.setDefaultNightMode(nightModeOf(read()))
}
