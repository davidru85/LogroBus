package com.ruizurraca.logrobus.core.network

import com.ruizurraca.logrobus.core.common.LogLevel
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.ServerCorrectedClock
import kotlin.time.Clock
import kotlin.time.Instant

/** The default `api_base_url`. Tests never call it: their engines are mocks (AGENTS.md §9). */
internal const val BASE_URL = "https://transporteurbano.logrono.es"

/** What the device clock reads in every test. */
internal val START: Instant = Instant.parse("2026-10-27T08:00:00Z")

/** The app clock on a device clock stopped at [START]. */
internal fun stoppedClock() =
    ServerCorrectedClock(
        object : Clock {
            override fun now() = START
        },
    )

/** Records each log entry as its level, tag, message and throwable. */
internal class RecordingLogger : Logger {
    val entries = mutableListOf<List<Any?>>()

    override fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        entries += listOf(level, tag, message, throwable)
    }
}
