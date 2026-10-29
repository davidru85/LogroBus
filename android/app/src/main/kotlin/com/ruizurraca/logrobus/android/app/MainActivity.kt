package com.ruizurraca.logrobus.android.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ruizurraca.logrobus.android.core.designsystem.LogroBusTheme

/** The single activity. Until the app shell lands (LB-012), it shows the start destination as a placeholder. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The bars follow the system's light or dark mode, as LogroBusTheme does. Below API 29, which has no contrast
        // enforcement, a scrim behind the navigation bar keeps its icons readable.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LogroBusTheme {
                Surface(Modifier.fillMaxSize()) { StartDestinationPlaceholder() }
            }
        }
    }
}
