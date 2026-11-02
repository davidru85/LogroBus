package com.ruizurraca.logrobus.core.database

import app.cash.sqldelight.db.SqlDriver

/**
 * The app's database on [driver], with the adapters of its typed columns. Create one per process (ADR-0013).
 * SQLDelight asks for a column's adapter only once a query reads or writes the column. The first such query on the
 * Instant columns of news or plan_recent therefore stops this function compiling until it passes that table's
 * InstantColumnAdapter too. Favourites' came with LB-103's insertFavourite.
 */
fun logroBusDatabase(driver: SqlDriver): LogroBusDatabase =
    LogroBusDatabase(
        driver = driver,
        favouriteAdapter = Favourite.Adapter(created_atAdapter = InstantColumnAdapter),
        sync_stateAdapter = Sync_state.Adapter(last_successAdapter = InstantColumnAdapter),
    )
