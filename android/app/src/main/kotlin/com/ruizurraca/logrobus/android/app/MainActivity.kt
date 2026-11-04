package com.ruizurraca.logrobus.android.app

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
    private val languages: AppCompatLanguages by inject()

    override fun attachBaseContext(newBase: Context) {
        // ADR-0020, the one blocking read outside tests (code_style.md §2.2, owner decision, 2026-11-03): below Android
        // 12, a few bytes once per process, so the first frame is in the saved theme.
        applySavedThemeBeforeTheFirstFrame { runBlocking { observePreferences().first().theme } }
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // The bars follow the app's light or dark mode, as LogroBusTheme does. Below API 29, which has no contrast
        // enforcement, a scrim behind the navigation bar keeps its icons readable.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // A language chosen in Android's own settings recreates the activity, while the process lives on.
        languages.refresh()
        // A theme chosen in Settings, which recreates this activity in it.
        lifecycleScope.launch {
            observePreferences().map { it.theme }.distinctUntilChanged().collect { applyTheme(this@MainActivity, it) }
        }
        setContent {
            LogroBusTheme { AppShell() }
        }
    }
}
