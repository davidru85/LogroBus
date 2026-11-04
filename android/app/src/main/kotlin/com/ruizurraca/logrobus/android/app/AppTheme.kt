package com.ruizurraca.logrobus.android.app

import android.annotation.SuppressLint
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
internal fun applicationNightModeOf(theme: ThemePreference): Int =
    when (theme) {
        ThemePreference.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
        ThemePreference.LIGHT -> UiModeManager.MODE_NIGHT_NO
        ThemePreference.DARK -> UiModeManager.MODE_NIGHT_YES
    }

/**
 * Applies [theme] to the whole app on an Android of version [sdk] (ADR-0020). From Android 12 the system keeps it as
 * the app's own night mode, which it applies before any of the app's code runs, starting window included; below,
 * AppCompat applies it. Either recreates the activity, once.
 *
 * [sdk] is the device's `Build.VERSION.SDK_INT`, a parameter only so that tests can pass each version; lint cannot see
 * that it guards the Android 12 call.
 */
@SuppressLint("NewApi")
internal fun applyTheme(
    context: Context,
    theme: ThemePreference,
    sdk: Int = Build.VERSION.SDK_INT,
) {
    if (sdk >= Build.VERSION_CODES.S) {
        context.getSystemService(UiModeManager::class.java)?.setApplicationNightMode(applicationNightModeOf(theme))
    } else {
        AppCompatDelegate.setDefaultNightMode(nightModeOf(theme))
    }
}

/**
 * Below Android 12, before the activity's first frame: the saved theme, which [read] gives, so the app never opens in
 * the other theme and then turns. AppCompat keeps it for the process, so it is read once, at the first creation, not
 * at each recreation. From Android 12 the system has already applied it.
 */
internal fun applySavedThemeBeforeTheFirstFrame(
    sdk: Int = Build.VERSION.SDK_INT,
    read: () -> ThemePreference,
) {
    if (sdk < Build.VERSION_CODES.S &&
        AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
    ) {
        AppCompatDelegate.setDefaultNightMode(nightModeOf(read()))
    }
}
