package com.ruizurraca.logrobus.core.database

import app.cash.sqldelight.db.SqlDriver

/**
 * The app's database on [driver], with the adapters of its typed columns. Create one per process (ADR-0013).
 * SQLDelight asks for a column's adapter only once a query reads or writes the column. The first such query on the
 * Instant columns of favourite, news or plan_recent therefore stops this function compiling until it passes that
 * table's InstantColumnAdapter too. The static sync's countMissingStops (LB-008) uses none of them.
 */
fun logroBusDatabase(driver: SqlDriver): LogroBusDatabase =
    LogroBusDatabase(
        driver = driver,
        sync_stateAdapter = Sync_state.Adapter(last_successAdapter = InstantColumnAdapter),
    )
