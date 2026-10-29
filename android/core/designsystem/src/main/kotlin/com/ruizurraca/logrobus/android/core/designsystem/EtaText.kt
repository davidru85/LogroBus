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
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
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
    val resources = LocalResources.current
    val lines = remember(eta, resources) { resources.etaLines(eta) }
    val spoken = remember(eta, resources) { resources.etaSpoken(eta) }
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val pulse = if (eta.display == EtaDisplay.Now && !eta.cancelled) rememberNowPulse() else null
    Column(modifier.clearAndSetSemantics { contentDescription = spoken }, horizontalAlignment = Alignment.End) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                lines.value,
                // Read while drawing, so the pulse redraws the text without recomposing the row.
                Modifier.graphicsLayer {
                    alpha = pulse?.let { nowAlpha(it.value, ValueAnimator.areAnimatorsEnabled()) } ?: 1f
                },
                color = if (eta.scheduled || eta.cancelled) secondary else LocalContentColor.current,
                style = etaStyle,
                textDecoration = if (eta.cancelled) TextDecoration.LineThrough else null,
            )
            // DD-09: a low-confidence ETA is marked, never hidden. The icon grows with the text.
            if (eta.lowConfidence) {
                Icon(
                    painterResource(R.drawable.ic_eta_low_confidence),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .padding(start = Tokens.Spacing.xs)
                            .size(with(LocalDensity.current) { etaStyle.fontSize.toDp() }),
                    tint = secondary,
                )
            }
        }
        lines.secondary?.let { Text(it, color = secondary, style = MaterialTheme.typography.bodySmall) }
    }
}

/**
 * "Now" pulses gently (design_guidelines.md §6.3). A tween rather than the effects spring of §4, because a spring
 * cannot repeat.
 */
@Composable
private fun rememberNowPulse(): State<Float> =
    rememberInfiniteTransition(label = "now").animateFloat(
        initialValue = 1f,
        targetValue = PULSE_MIN_ALPHA,
        animationSpec = infiniteRepeatable(tween(PULSE_MILLIS), RepeatMode.Reverse),
        label = "alpha",
    )

/** The alpha of "Now" for a [pulse] value: the pulse while animations are on, still at 1 when they are off. */
internal fun nowAlpha(
    pulse: Float,
    animationsEnabled: Boolean,
): Float = if (animationsEnabled) pulse else 1f

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
    return EtaLines(if (eta.lowConfidence) getString(R.string.eta_approximate, value) else value, secondary)
}

/**
 * What TalkBack says for an ETA, without the line. It says what the screen shows: the time, then the secondary line's
 * qualifier, with the same precedence as [etaLines].
 */
internal fun Resources.etaSpoken(eta: EtaUi): String {
    if (eta.cancelled) return getString(R.string.eta_cancelled_spoken)
    val display = eta.display
    val time =
        when (display) {
            EtaDisplay.Now -> {
                getString(if (eta.lowConfidence) R.string.eta_now_approximately_spoken else R.string.eta_now_spoken)
            }

            is EtaDisplay.Minutes -> {
                getQuantityString(R.plurals.eta_minutes_spoken, display.minutes, display.minutes)
            }

            is EtaDisplay.ClockTime -> {
                val clock = if (eta.scheduled) R.plurals.eta_scheduled_spoken else R.plurals.eta_clock_spoken
                getQuantityString(clock, display.hour, display.text)
            }
        }
    val approximate =
        if (eta.lowConfidence && display != EtaDisplay.Now) getString(R.string.eta_approximately_spoken, time) else time
    val qualifier =
        when {
            // A scheduled clock time already says "scheduled at".
            eta.scheduled -> {
                if (display is EtaDisplay.ClockTime) {
                    null
                } else {
                    getString(
                        R.string.eta_scheduled_short_spoken,
                    )
                }
            }

            else -> {
                eta.delay?.let(::delaySpoken)
            }
        }
    return qualifier?.let { getString(R.string.eta_spoken_join, approximate, it) } ?: approximate
}

private fun Resources.delayText(delay: EtaDelay): String =
    when (delay) {
        is EtaDelay.Late -> getString(R.string.eta_late, delay.minutes)
        is EtaDelay.Early -> getString(R.string.eta_early, delay.minutes)
    }

private fun Resources.delaySpoken(delay: EtaDelay): String =
    when (delay) {
        is EtaDelay.Late -> getQuantityString(R.plurals.eta_late_spoken, delay.minutes, delay.minutes)
        is EtaDelay.Early -> getQuantityString(R.plurals.eta_early_spoken, delay.minutes, delay.minutes)
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

// The pulse's faintest alpha keeps "Now" at 4.5:1 or more on every surface (lb011_thePulseKeepsNowReadable).
internal const val PULSE_MIN_ALPHA = 0.7f
private const val PULSE_MILLIS = 900
