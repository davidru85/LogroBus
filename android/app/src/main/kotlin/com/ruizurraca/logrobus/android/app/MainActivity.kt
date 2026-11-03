package com.ruizurraca.logrobus.android.app

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.ruizurraca.logrobus.android.core.designsystem.LogroBusTheme
import com.ruizurraca.logrobus.feature.settings.domain.ObservePreferences
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

/**
 * The single activity, which draws the app shell edge-to-edge (LB-012). An AppCompatActivity, so that the language
 * and theme chosen in Settings apply to it (ADR-0020).
 */
class MainActivity : AppCompatActivity() {
    private val observePreferences: ObservePreferences by inject()

    override fun attachBaseContext(newBase: Context) {
        // FR-SET-01: the saved theme before the first frame, so the app never opens in the other one and then turns.
        // The settings file is a few bytes, read once per launch.
        val theme = runBlocking { observePreferences().first().theme }
        AppCompatDelegate.setDefaultNightMode(nightModeOf(theme))
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // The bars follow the app's light or dark mode, as LogroBusTheme does. Below API 29, which has no contrast
        // enforcement, a scrim behind the navigation bar keeps its icons readable.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // A theme chosen in Settings: AppCompat recreates this activity in it.
        lifecycleScope.launch {
            observePreferences().map { nightModeOf(it.theme) }.distinctUntilChanged().collect {
                AppCompatDelegate.setDefaultNightMode(it)
            }
        }
        setContent {
            LogroBusTheme { AppShell() }
        }
    }
}
