package com.ruizurraca.logrobus.core.database

import app.cash.sqldelight.ColumnAdapter
import kotlin.time.Instant

/** AGENTS.md §5.5: an instant column holds UTC epoch milliseconds, the one unit every query compares in. */
internal object InstantColumnAdapter : ColumnAdapter<Instant, Long> {
    override fun decode(databaseValue: Long): Instant = Instant.fromEpochMilliseconds(databaseValue)

    override fun encode(value: Instant): Long = value.toEpochMilliseconds()
}
