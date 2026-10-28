package com.ruizurraca.logrobus.android.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * The app's theme (design_guidelines.md §4): the [Tokens] schemes for light and dark, without dynamic colour, so brand
 * surfaces stay predictable next to the data-driven line colours.
 */
@Composable
fun LogroBusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(content = content)
}
