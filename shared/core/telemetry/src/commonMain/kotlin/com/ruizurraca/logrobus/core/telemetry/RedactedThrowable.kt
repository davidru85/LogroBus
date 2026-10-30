package com.ruizurraca.logrobus.core.telemetry

class RedactedThrowable internal constructor(
    val type: String,
    val frames: List<String>,
    cause: RedactedThrowable?,
) : Throwable(type, cause)
