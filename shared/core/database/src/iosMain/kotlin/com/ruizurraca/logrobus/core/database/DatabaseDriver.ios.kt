package com.ruizurraca.logrobus.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/** The driver of the app's database on iOS, in the app's Application Support directory (ADR-0013). */
fun logroBusDriver(name: String = DATABASE_NAME): SqlDriver = NativeSqliteDriver(LogroBusDatabase.Schema, name)
