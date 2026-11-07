package com.ruizurraca.logrobus.android.core.designsystem

import java.util.Locale

/**
 * Spec §5.7: a time as HH:mm on the Madrid clock, the same digits in every locale, as ETAs show clock times and the
 * planner its departure (LB-303).
 */
fun clockText(
    hour: Int,
    minute: Int,
): String = String.format(Locale.ROOT, "%02d:%02d", hour, minute)
