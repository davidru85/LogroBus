package com.ruizurraca.logrobus.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/** The driver of the app's database on iOS, in the app's Application Support directory (ADR-0013). */
fun logroBusDriver(): SqlDriver = logroBusDriver(DATABASE_NAME)

/** The same on the file [name], so a test can use a throwaway file. */
internal fun logroBusDriver(name: String): SqlDriver = NativeSqliteDriver(LogroBusDatabase.Schema, name)
