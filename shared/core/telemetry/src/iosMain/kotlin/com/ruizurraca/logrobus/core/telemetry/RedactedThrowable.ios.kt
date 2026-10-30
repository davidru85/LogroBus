package com.ruizurraca.logrobus.core.telemetry

import kotlin.experimental.ExperimentalNativeApi

// Kotlin/Native's only way to read an error's frames without its message.
@OptIn(ExperimentalNativeApi::class)
internal actual fun Throwable.stackFrames(): List<String> = getStackTrace().toList()

// Kotlin/Native records a stack only when an error is created, so a vendor implementation reads `frames` instead.
internal actual fun RedactedThrowable.adoptStackOf(original: Throwable) = Unit
