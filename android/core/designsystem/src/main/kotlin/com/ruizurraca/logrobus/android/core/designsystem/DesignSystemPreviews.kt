@file:Suppress("MagicNumber") // Sample ETAs and a line colour for the previews.

package com.ruizurraca.logrobus.android.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ruizurraca.logrobus.core.presentation.EtaDelay
import com.ruizurraca.logrobus.core.presentation.EtaDisplay
import com.ruizurraca.logrobus.core.presentation.EtaUi

// Previews of the components in light, dark and large fonts. The screenshot tests cover every case.

@PreviewLightDark
@PreviewFontScale
@Composable
private fun LineBadgePreview() {
    LogroBusTheme {
        Surface {
            Row(Modifier.padding(Tokens.Spacing.s), horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)) {
                LineBadgeSize.entries.forEach { size -> LineBadge("B1", PREVIEW_RED, PREVIEW_ON_RED, size = size) }
            }
        }
    }
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun EtaTextPreview() {
    LogroBusTheme {
        Surface {
            Column(
                Modifier.padding(Tokens.Spacing.s),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.s),
                horizontalAlignment = Alignment.End,
            ) {
                EtaText(
                    EtaUi(EtaDisplay.Now, lowConfidence = false, scheduled = false, delay = null, cancelled = false),
                )
                EtaText(
                    EtaUi(
                        EtaDisplay.Minutes(4),
                        lowConfidence = true,
                        scheduled = false,
                        delay = EtaDelay.Late(3),
                        cancelled = false,
                    ),
                )
                EtaText(
                    EtaUi(
                        EtaDisplay.ClockTime(9, 5),
                        lowConfidence = false,
                        scheduled = true,
                        delay = null,
                        cancelled = false,
                    ),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun TopAppBarActionsPreview() {
    LogroBusTheme {
        Surface {
            Row {
                UpButton(onClick = {})
                MoreActionButton(onClick = {})
            }
        }
    }
}

// Line B1's red and its onColor (spec §3).
private const val PREVIEW_RED = 0xFFFD0200
private const val PREVIEW_ON_RED = 0xFF000000
