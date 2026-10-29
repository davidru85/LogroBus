package com.ruizurraca.logrobus.android.core.designsystem

import com.ruizurraca.logrobus.core.common.color.onColorFor

/** Spec §3's line catalogue as of 2026-10: each code with its API-01 colour and the onColor the data layer gives it. */
internal val currentLines: List<Triple<String, Long, Long>> =
    listOf(
        "1" to 0xFF269905,
        "2" to 0xFFFFFF00,
        "3" to 0xFFC100FB,
        "4" to 0xFFFD0200,
        "5" to 0xFFFD82BB,
        "6" to 0xFF924C48,
        "7" to 0xFFBABABA,
        "9" to 0xFF0002FD,
        "10" to 0xFF0FDCFC,
        "11" to 0xFFFFE07D,
        "B1" to 0xFFFD0200,
        "B2" to 0xFF0002FD,
        "B3" to 0xFF01FF01,
    ).map { (code, color) -> Triple(code, color, onColorFor(color)) }
