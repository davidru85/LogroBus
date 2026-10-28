package com.ruizurraca.logrobus.android.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * The only way a line is shown (design_guidelines.md §6.2): its [code] in [onColor] on its [color], both opaque ARGB
 * values from the data (DD-08, NFR-A11Y-06). Lines 4 and B1 share a colour, as do 9 and B2, so the code always shows.
 * It grows with the font scale rather than truncate, and is spoken as its line, never its colour.
 */
@Composable
fun LineBadge(
    code: String,
    color: Long,
    onColor: Long,
    modifier: Modifier = Modifier,
    size: LineBadgeSize = LineBadgeSize.Regular,
) {
    val label = stringResource(R.string.line_badge, code)
    Box(
        modifier
            .clearAndSetSemantics { contentDescription = label }
            .defaultMinSize(minWidth = size.minWidth, minHeight = size.minHeight)
            .background(Color(color), RoundedCornerShape(Tokens.lineBadgeCorner))
            .padding(horizontal = size.padding, vertical = Tokens.lineBadgeVerticalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(code, color = Color(onColor), style = size.textStyle, fontWeight = FontWeight.Bold, softWrap = false)
    }
}

private val LineBadgeSize.textStyle: TextStyle
    @Composable get() =
        when (this) {
            LineBadgeSize.Small -> MaterialTheme.typography.labelMedium
            LineBadgeSize.Regular -> MaterialTheme.typography.labelLarge
            LineBadgeSize.Large -> MaterialTheme.typography.titleMedium
        }
