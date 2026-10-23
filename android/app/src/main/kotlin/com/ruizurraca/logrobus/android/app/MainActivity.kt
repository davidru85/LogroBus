package com.ruizurraca.logrobus.android.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/** The single activity. Until the app shell lands (LB-012), it shows the start destination as a placeholder. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // The placeholder is light-only, so the bar icons stay dark in dark mode too. LB-011 switches back to
        // SystemBarStyle.auto when the light and dark brand theme lands.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            // The dark scrim keeps the light navigation icons readable on API 26, which cannot draw dark ones.
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, NAVIGATION_BAR_DARK_SCRIM),
        )
        super.onCreate(savedInstanceState)
        setContent { StartDestinationPlaceholder() }
    }

    private companion object {
        val NAVIGATION_BAR_DARK_SCRIM = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
