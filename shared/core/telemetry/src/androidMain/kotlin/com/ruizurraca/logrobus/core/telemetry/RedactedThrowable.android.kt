package com.ruizurraca.logrobus.core.telemetry

internal actual fun Throwable.stackFrames(): List<String> = stackTrace.map(StackTraceElement::toString)

internal actual fun RedactedThrowable.adoptStackOf(original: Throwable) {
    stackTrace = original.stackTrace
}
