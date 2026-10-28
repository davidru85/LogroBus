package com.ruizurraca.logrobus.android.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The colours Material's scheme has no role for (design_guidelines.md §6.1), read through [LogroBusTheme]. */
@Immutable
data class StatusColors(
    /** Text that warns, such as "Actualizado hace N s" on stale data (spec §7.3). */
    val warning: Color,
)

internal val LocalStatusColors = staticCompositionLocalOf { Tokens.lightStatus }
