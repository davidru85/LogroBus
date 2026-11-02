package com.ruizurraca.logrobus.core.network

/**
 * DD-07: the source sends `""` where it means "no value", in ET's `aimedDepartureTime` and VM's `directionRef` and
 * `nextStopRef` among others. Every mapper reads its strings through this first (spec §5.4, step 2): a missing,
 * empty or blank string is `null`, and any other is trimmed.
 */
fun String?.trimmedOrNull(): String? = this?.trim()?.ifEmpty { null }
