package com.ruizurraca.logrobus.android.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** design_guidelines.md §4 and §6.1: the theme draws on the tokens, in light and dark. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LogroBusThemeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun lb011_theThemeUsesTheTokensSchemes_inLightAndDark() {
        var light: ColorScheme? = null
        var dark: ColorScheme? = null
        compose.setContent {
            LogroBusTheme(darkTheme = false) {
                light = MaterialTheme.colorScheme
                LogroBusTheme(darkTheme = true) { dark = MaterialTheme.colorScheme }
            }
        }

        assertEquals(Tokens.lightColors.surface, light?.surface)
        assertEquals(Tokens.lightColors.primary, light?.primary)
        assertEquals(Tokens.darkColors.surface, dark?.surface)
        assertEquals(Tokens.darkColors.primary, dark?.primary)
    }

    @Test
    fun lb011_theThemeGivesTheStatusColours_inLightAndDark() {
        var light: StatusColors? = null
        var dark: StatusColors? = null
        compose.setContent {
            LogroBusTheme(darkTheme = false) {
                light = LogroBusTheme.statusColors
                LogroBusTheme(darkTheme = true) { dark = LogroBusTheme.statusColors }
            }
        }

        assertEquals(Tokens.lightStatus, light)
        assertEquals(Tokens.darkStatus, dark)
    }

    @Test
    fun lb011_cardsHave12dpCorners() {
        var shapes: Shapes? = null
        compose.setContent { LogroBusTheme { shapes = MaterialTheme.shapes } }

        assertEquals(RoundedCornerShape(12.dp), shapes?.medium)
    }
}
