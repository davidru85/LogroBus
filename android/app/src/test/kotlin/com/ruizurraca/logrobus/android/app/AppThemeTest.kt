package com.ruizurraca.logrobus.android.app

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.logrobus.core.preferences.ThemePreference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * FR-SET-01 and ADR-0020: the theme applies to the whole app, through AppCompat below Android 12, and through the
 * system's per-app night mode from Android 12, which also themes the starting window. Robolectric runs one Android,
 * so the version is passed in.
 */
@RunWith(AndroidJUnit4::class)
class AppThemeTest {
    private val context = RuntimeEnvironment.getApplication()
    private val uiModes = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager

    @Before
    @After
    fun forget() = forgetAppCompatsNightMode()

    @Test
    fun lb403_eachThemeIsAppCompatsNightMode_andTheSystemsPerAppNightMode() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, nightModeOf(ThemePreference.SYSTEM))
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, nightModeOf(ThemePreference.LIGHT))
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, nightModeOf(ThemePreference.DARK))
        assertEquals(UiModeManager.MODE_NIGHT_AUTO, applicationNightModeOf(ThemePreference.SYSTEM))
        assertEquals(UiModeManager.MODE_NIGHT_NO, applicationNightModeOf(ThemePreference.LIGHT))
        assertEquals(UiModeManager.MODE_NIGHT_YES, applicationNightModeOf(ThemePreference.DARK))
    }

    @Test
    fun lb403_belowAndroid12_appCompatAppliesTheTheme() {
        applyTheme(context, ThemePreference.DARK, sdk = Build.VERSION_CODES.R)

        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())
        assertEquals(UiModeManager.MODE_NIGHT_AUTO, shadowOf(uiModes).applicationNightMode)
    }

    @Test
    fun lb403_fromAndroid12_theSystemAppliesTheTheme_startingWindowIncluded() {
        applyTheme(context, ThemePreference.DARK, sdk = Build.VERSION_CODES.S)

        assertEquals(UiModeManager.MODE_NIGHT_YES, shadowOf(uiModes).applicationNightMode)
        // Left to the system, so the activity is recreated once, not once for each.
        assertEquals(AppCompatDelegate.MODE_NIGHT_UNSPECIFIED, AppCompatDelegate.getDefaultNightMode())
    }

    @Test
    fun lb403_belowAndroid12_theSavedThemeIsReadOncePerProcess_beforeTheFirstFrame() {
        var reads = 0
        val read = {
            reads++
            ThemePreference.DARK
        }

        applySavedThemeBeforeTheFirstFrame(sdk = Build.VERSION_CODES.R, read)
        // A rotation, a new theme or a new language recreates the activity: AppCompat already has the theme.
        applySavedThemeBeforeTheFirstFrame(sdk = Build.VERSION_CODES.R, read)

        assertEquals(1, reads)
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())
    }

    @Test
    fun lb403_fromAndroid12_nothingIsReadBeforeTheFirstFrame() {
        // The system keeps the app's night mode across launches and applies it before any code runs.
        applySavedThemeBeforeTheFirstFrame(sdk = Build.VERSION_CODES.S) { error("Nothing should be read") }

        assertEquals(AppCompatDelegate.MODE_NIGHT_UNSPECIFIED, AppCompatDelegate.getDefaultNightMode())
    }
}
