package com.ruizurraca.logrobus.transit.data

import app.cash.sqldelight.db.SqlDriver

/** A new in-memory database with the schema, on this platform's test driver. */
internal expect fun testDriver(): SqlDriver
