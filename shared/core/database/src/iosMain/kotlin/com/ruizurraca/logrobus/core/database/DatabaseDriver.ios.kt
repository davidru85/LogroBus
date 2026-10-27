package com.ruizurraca.logrobus.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver

/** The driver of the app's database on iOS. */
fun logroBusDriver(name: String = DATABASE_NAME): SqlDriver = inMemoryDriver(LogroBusDatabase.Schema)
