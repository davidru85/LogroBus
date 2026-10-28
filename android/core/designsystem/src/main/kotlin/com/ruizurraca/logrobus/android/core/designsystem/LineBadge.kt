package com.ruizurraca.logrobus.android.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** The sizes of a [LineBadge]; each is at least 32 × 24 dp (design_guidelines.md §6.1). */
enum class LineBadgeSize { Small, Regular, Large }

/**
 * The only way a line is shown (design_guidelines.md §6.2): its [code] in [onColor] on its [color], both opaque ARGB
 * values from the data (DD-08, NFR-A11Y-06). Lines 4 and B1 share a colour, as do 9 and B2, so the code always shows.
 */
@Composable
fun LineBadge(
    code: String,
    color: Long,
    onColor: Long,
    modifier: Modifier = Modifier,
    size: LineBadgeSize = LineBadgeSize.Regular,
) {
}
