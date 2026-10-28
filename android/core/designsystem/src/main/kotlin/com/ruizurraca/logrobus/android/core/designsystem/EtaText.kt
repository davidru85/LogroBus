package com.ruizurraca.logrobus.android.core.designsystem

import android.animation.ValueAnimator
import android.content.res.Resources
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ruizurraca.logrobus.core.presentation.EtaDelay
import com.ruizurraca.logrobus.core.presentation.EtaDisplay
import com.ruizurraca.logrobus.core.presentation.EtaUi
import java.util.Locale

/**
 * An arrival's ETA, drawn and spoken as design_guidelines.md §6.3 says. Its spoken label leaves out the line, which
 * the row's [LineBadge] says, so a row reads "Line 2, in 4 minutes".
 */
@Composable
fun EtaText(
    eta: EtaUi,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources
    val lines = resources.etaLines(eta)
    val spoken = resources.etaSpoken(eta)
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val alpha = if (eta.display == EtaDisplay.Now && !eta.cancelled) nowPulseAlpha() else 1f
    Column(modifier.clearAndSetSemantics { contentDescription = spoken }, horizontalAlignment = Alignment.End) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                lines.value,
                Modifier.graphicsLayer { this.alpha = alpha },
                color = if (eta.scheduled || eta.cancelled) secondary else LocalContentColor.current,
                style = etaStyle,
                textDecoration = if (eta.cancelled) TextDecoration.LineThrough else null,
            )
            // DD-09: a low-confidence ETA is marked, never hidden.
            if (eta.lowConfidence) {
                Icon(
                    painterResource(R.drawable.ic_eta_low_confidence),
                    contentDescription = null,
                    modifier = Modifier.padding(start = Tokens.Spacing.xs).size(LOW_CONFIDENCE_ICON.dp),
                    tint = secondary,
                )
            }
        }
        lines.secondary?.let { Text(it, color = secondary, style = MaterialTheme.typography.bodySmall) }
    }
}

/** "Now" pulses gently, and holds still when the system's animations are off (design_guidelines.md §6.3). */
@Composable
private fun nowPulseAlpha(): Float {
    val pulse by rememberInfiniteTransition(label = "now").animateFloat(
        initialValue = 1f,
        targetValue = PULSE_MIN_ALPHA,
        animationSpec = infiniteRepeatable(tween(PULSE_MILLIS), RepeatMode.Reverse),
        label = "alpha",
    )
    return if (ValueAnimator.areAnimatorsEnabled()) pulse else 1f
}

/** What an ETA shows: its value, and the secondary line under it, if any. */
internal data class EtaLines(
    val value: String,
    val secondary: String?,
)

internal fun Resources.etaLines(eta: EtaUi): EtaLines {
    val value =
        when (val display = eta.display) {
            EtaDisplay.Now -> getString(R.string.eta_now)
            is EtaDisplay.Minutes -> getString(R.string.eta_minutes, display.minutes)
            is EtaDisplay.ClockTime -> display.text
        }
    val secondary =
        when {
            eta.cancelled -> getString(R.string.eta_cancelled)
            eta.scheduled -> getString(R.string.eta_scheduled)
            else -> eta.delay?.let(::delayText)
        }
    return EtaLines(if (eta.lowConfidence) "~$value" else value, secondary)
}

/** What TalkBack says for an ETA, without the line. */
internal fun Resources.etaSpoken(eta: EtaUi): String {
    if (eta.cancelled) return getString(R.string.eta_cancelled_spoken)
    val time =
        when (val display = eta.display) {
            EtaDisplay.Now -> {
                getString(R.string.eta_now_spoken)
            }

            is EtaDisplay.Minutes -> {
                getQuantityString(R.plurals.eta_minutes_spoken, display.minutes, display.minutes)
            }

            is EtaDisplay.ClockTime -> {
                getString(if (eta.scheduled) R.string.eta_scheduled_spoken else R.string.eta_clock_spoken, display.text)
            }
        }
    val approximate = if (eta.lowConfidence) getString(R.string.eta_approximately_spoken, time) else time
    val delay =
        when (val delay = eta.delay) {
            is EtaDelay.Late -> getQuantityString(R.plurals.eta_late_spoken, delay.minutes, delay.minutes)
            is EtaDelay.Early -> getQuantityString(R.plurals.eta_early_spoken, delay.minutes, delay.minutes)
            null -> null
        }
    return listOfNotNull(approximate, delay).joinToString(", ")
}

private fun Resources.delayText(delay: EtaDelay): String =
    when (delay) {
        is EtaDelay.Late -> getString(R.string.eta_late, delay.minutes)
        is EtaDelay.Early -> getString(R.string.eta_early, delay.minutes)
    }

// Spec §5.7: HH:mm on the Madrid clock, the same digits in every locale.
private val EtaDisplay.ClockTime.text: String
    get() = String.format(Locale.ROOT, "%02d:%02d", hour, minute)

/**
 * ETAs use the emphasized title with tabular figures, so digits do not shift as they tick
 * (design_guidelines.md §4).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class) // Expressive APIs stay inside the design system (ADR-0005).
internal val etaStyle: TextStyle
    @Composable get() = MaterialTheme.typography.titleMediumEmphasized.copy(fontFeatureSettings = "tnum")

private const val LOW_CONFIDENCE_ICON = 16
private const val PULSE_MIN_ALPHA = 0.55f
private const val PULSE_MILLIS = 900
