package com.ruizurraca.logrobus.core.presentation

import com.ruizurraca.logrobus.core.preferences.EtaFormat
import kotlin.time.Instant

/**
 * When an arrival comes, as spec §5.7 shows it. Shared code decides the case; each platform words it, draws it and
 * speaks it (AGENTS.md §5, rule 4; design_guidelines.md §6.3).
 */
sealed interface EtaDisplay {
    /** Under 60 s away. */
    data object Now : EtaDisplay

    /** From 1 to 59 [minutes] away, rounded down. */
    data class Minutes(
        val minutes: Int,
    ) : EtaDisplay

    /** An hour or more away, a scheduled arrival, or expired data: a time on the Madrid clock, shown as HH:mm. */
    data class ClockTime(
        val hour: Int,
        val minute: Int,
    ) : EtaDisplay

    companion object {
        /** Spec §5.7: how an arrival [expected] at that instant shows at [now], in the user's ETA [format] (FR-SET-01). */
        @Suppress("UNUSED_PARAMETER", "UnusedParameter")
        fun of(
            expected: Instant,
            now: Instant,
            format: EtaFormat,
        ): EtaDisplay = Now
    }
}

/** How far an arrival runs from its schedule, once it is worth saying (spec §5.7). */
sealed interface EtaDelay {
    /** Two minutes or more late. */
    data class Late(
        val minutes: Int,
    ) : EtaDelay

    /** A minute or more early. */
    data class Early(
        val minutes: Int,
    ) : EtaDelay
}

/**
 * An arrival's ETA with its qualifiers (spec §5.7):
 * - [lowConfidence]: a "~" prefix and an info icon, never hidden (DD-09);
 * - [scheduled]: no real-time prediction, so a clock time in the secondary colour;
 * - [delay]: late or early, or null;
 * - [cancelled]: struck through.
 */
data class EtaUi(
    val display: EtaDisplay,
    val lowConfidence: Boolean,
    val scheduled: Boolean,
    val delay: EtaDelay?,
    val cancelled: Boolean,
)
