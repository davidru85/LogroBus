package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.common.LogLevel
import com.ruizurraca.logrobus.core.common.Logger

/** Keeps every entry, for tests that check what was logged. */
internal class RecordingLogger : Logger {
    val entries = mutableListOf<Entry>()

    override fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        entries += Entry(level, tag, message, throwable)
    }

    data class Entry(
        val level: LogLevel,
        val tag: String,
        val message: String,
        val throwable: Throwable?,
    )
}
