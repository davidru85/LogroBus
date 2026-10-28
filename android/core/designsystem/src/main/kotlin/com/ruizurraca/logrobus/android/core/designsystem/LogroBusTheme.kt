package com.ruizurraca.logrobus.android.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * The app's theme (design_guidelines.md §4): Material 3 Expressive, with its expressive motion, on the [Tokens]
 * schemes for light and dark. There is no dynamic colour, so brand surfaces stay predictable next to the data-driven
 * line colours.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class) // Expressive APIs stay inside the design system (ADR-0005).
@Composable
fun LogroBusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalStatusColors provides if (darkTheme) Tokens.darkStatus else Tokens.lightStatus) {
        MaterialExpressiveTheme(
            colorScheme = if (darkTheme) Tokens.darkColors else Tokens.lightColors,
            motionScheme = MotionScheme.expressive(),
            shapes = Tokens.shapes,
            typography = Tokens.typography,
            content = content,
        )
    }
}

/** The theme's values beyond MaterialTheme's. */
object LogroBusTheme {
    val statusColors: StatusColors
        @Composable @ReadOnlyComposable
        get() = LocalStatusColors.current
}
