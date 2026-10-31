package com.ruizurraca.logrobus.core.common

import kotlinx.datetime.TimeZone

/**
 * Logroño's time zone, CET in winter and CEST in summer (AGENTS.md §5.5, ADR-0015). The one place that names it, for
 * the data layer's DD-05 fix and the presentation layer's wall-clock times alike.
 */
val madridTimeZone: TimeZone = TimeZone.of("Europe/Madrid")
