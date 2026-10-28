package com.ruizurraca.logrobus.android.core.designsystem

import android.content.res.Resources
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.ruizurraca.logrobus.core.presentation.EtaUi

/**
 * An arrival's ETA, drawn and spoken as design_guidelines.md §6.3 says. Its spoken label leaves out the line, which
 * the row's [LineBadge] says, so a row reads "Line 2, in 4 minutes".
 */
@Composable
fun EtaText(
    eta: EtaUi,
    modifier: Modifier = Modifier,
) {
}

/** What an ETA shows: its value, and the secondary line under it, if any. */
internal data class EtaLines(
    val value: String,
    val secondary: String?,
)

internal fun Resources.etaLines(eta: EtaUi): EtaLines = EtaLines("", null)

/** What TalkBack says for an ETA, without the line. */
internal fun Resources.etaSpoken(eta: EtaUi): String = ""

/** ETAs use the emphasized title with tabular figures, so digits do not shift as they tick (design_guidelines.md §4). */
internal val etaStyle: TextStyle
    @Composable get() = MaterialTheme.typography.titleMedium
