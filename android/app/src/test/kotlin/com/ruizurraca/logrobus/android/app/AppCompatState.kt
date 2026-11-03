package com.ruizurraca.logrobus.android.app

import androidx.appcompat.app.AppCompatDelegate

/**
 * Forgets AppCompat's night mode, which it keeps for the whole process, as a cold start finds it. Robolectric runs a
 * module's tests in one process, and setDefaultNightMode refuses MODE_NIGHT_UNSPECIFIED, so this sets its field.
 */
internal fun forgetAppCompatsNightMode() {
    AppCompatDelegate::class.java
        .getDeclaredField("sDefaultNightMode")
        .apply { isAccessible = true }
        .setInt(null, AppCompatDelegate.MODE_NIGHT_UNSPECIFIED)
}
