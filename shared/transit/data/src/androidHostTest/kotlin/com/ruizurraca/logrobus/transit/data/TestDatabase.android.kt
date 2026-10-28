package com.ruizurraca.logrobus.transit.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.ruizurraca.logrobus.core.database.LogroBusDatabase

internal actual fun testDriver(): SqlDriver =
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { LogroBusDatabase.Schema.create(it) }
