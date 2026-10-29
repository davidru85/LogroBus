package com.ruizurraca.logrobus.android.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ruizurraca.logrobus.android.core.designsystem.LogroBusTheme

/** The single activity, which draws the app shell edge-to-edge (LB-012). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The bars follow the system's light or dark mode, as LogroBusTheme does. Below API 29, which has no contrast
        // enforcement, a scrim behind the navigation bar keeps its icons readable.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LogroBusTheme { AppShell() }
        }
    }
}
