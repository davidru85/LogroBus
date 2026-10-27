package com.ruizurraca.logrobus.core.database

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import kotlin.time.Instant

/** How instant columns are stored. */
internal object InstantColumnAdapter : ColumnAdapter<Instant, Long> {
    override fun decode(databaseValue: Long): Instant = Instant.fromEpochSeconds(databaseValue)

    override fun encode(value: Instant): Long = value.epochSeconds
}

/** The app's database on [driver]. */
fun logroBusDatabase(driver: SqlDriver): LogroBusDatabase =
    LogroBusDatabase(driver, sync_stateAdapter = Sync_state.Adapter(last_successAdapter = InstantColumnAdapter))
