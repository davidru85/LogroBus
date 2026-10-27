package com.ruizurraca.logrobus.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver

internal actual fun testDriver(): SqlDriver = inMemoryDriver(LogroBusDatabase.Schema)
