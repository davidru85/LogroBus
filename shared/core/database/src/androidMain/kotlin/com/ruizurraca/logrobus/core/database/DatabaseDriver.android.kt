package com.ruizurraca.logrobus.core.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/** The driver of the app's database on Android, in the app's databases directory (ADR-0013). */
fun logroBusDriver(context: Context): SqlDriver = AndroidSqliteDriver(LogroBusDatabase.Schema, context, DATABASE_NAME)
