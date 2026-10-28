package com.ruizurraca.logrobus.core.presentation

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * A time on the Madrid wall clock (AGENTS.md §5.5, ADR-0015), whatever the device time zone. Plain fields, so each
 * platform formats it with its own locale (AGENTS.md §5, rule 4).
 */
data class MadridDateTime(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
) {
    companion object {
        private val madrid = TimeZone.of("Europe/Madrid")

        fun of(instant: Instant): MadridDateTime {
            val local = instant.toLocalDateTime(madrid)
            return MadridDateTime(local.year, local.month.number, local.day, local.hour, local.minute)
        }
    }
}
