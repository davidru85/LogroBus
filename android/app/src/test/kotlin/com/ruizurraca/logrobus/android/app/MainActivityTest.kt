package com.ruizurraca.logrobus.android.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** LB-001: the app launches without crashing, on the start tab of FR-NAV-02, in both shipped languages. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MainActivityTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    @Config(qualifiers = "en")
    fun lb001_appLaunchesOnTheLinesTab() {
        compose.onNodeWithText("Lines").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "es")
    fun lb001_appLaunchesOnTheLinesTab_inSpanish() {
        compose.onNodeWithText("Líneas").assertIsDisplayed()
    }
}
