package com.ruizurraca.logrobus.android.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/** The single activity. Until the app shell lands (LB-012), it shows the start destination as a placeholder. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { StartDestinationPlaceholder() }
    }
}
