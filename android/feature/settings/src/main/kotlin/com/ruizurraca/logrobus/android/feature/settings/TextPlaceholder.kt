package com.ruizurraca.logrobus.android.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics

/** A line-high bar where text will be, hidden from accessibility services. */
@Composable
internal fun TextPlaceholder() {
    val lineHeight =
        with(LocalDensity.current) {
            MaterialTheme.typography.bodyMedium.lineHeight
                .toDp()
        }
    Box(
        Modifier
            .fillMaxWidth(PLACEHOLDER_WIDTH)
            .height(lineHeight)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clearAndSetSemantics {},
    )
}

// A placeholder spans this share of its row: long enough to read as text, short enough not to look like content.
private const val PLACEHOLDER_WIDTH = 0.4f
