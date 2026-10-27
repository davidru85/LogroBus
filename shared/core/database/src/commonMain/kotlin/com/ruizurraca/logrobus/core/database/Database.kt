package com.ruizurraca.logrobus.core.database

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import kotlin.time.Instant

/** AGENTS.md §5.5: an instant column holds UTC epoch milliseconds, the one unit every query compares in. */
internal object InstantColumnAdapter : ColumnAdapter<Instant, Long> {
    override fun decode(databaseValue: Long): Instant = Instant.fromEpochMilliseconds(databaseValue)

    override fun encode(value: Instant): Long = value.toEpochMilliseconds()
}

/**
 * The app's database on [driver], with the adapters of its typed columns. Create one per process (ADR-0013).
 * SQLDelight asks for a table's adapters once a query reads or writes it, so the instant columns of favourite, news
 * and plan_recent join here with their first queries; until then this function does not compile without them.
 */
fun logroBusDatabase(driver: SqlDriver): LogroBusDatabase =
    LogroBusDatabase(
        driver = driver,
        sync_stateAdapter = Sync_state.Adapter(last_successAdapter = InstantColumnAdapter),
    )
