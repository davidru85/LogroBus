package com.ruizurraca.logrobus.android.core.designsystem

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The sizes of a [LineBadge]; each is at least 32 × 24 dp (design_guidelines.md §6.1). */
@Suppress("MagicNumber") // Design tokens: the values are the point (code_style.md §2.1).
enum class LineBadgeSize(
    internal val minWidth: Dp,
    internal val minHeight: Dp,
    internal val padding: Dp,
) {
    Small(32.dp, 24.dp, 4.dp),
    Regular(40.dp, 28.dp, 6.dp),
    Large(48.dp, 32.dp, 8.dp),
}
