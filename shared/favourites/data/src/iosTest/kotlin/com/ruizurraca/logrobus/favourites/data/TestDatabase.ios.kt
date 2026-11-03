package com.ruizurraca.logrobus.favourites.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import com.ruizurraca.logrobus.core.database.LogroBusDatabase

internal actual fun testDriver(): SqlDriver = inMemoryDriver(LogroBusDatabase.Schema)
