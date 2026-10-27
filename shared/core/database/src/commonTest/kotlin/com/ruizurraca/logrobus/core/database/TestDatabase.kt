package com.ruizurraca.logrobus.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver

/** A new in-memory database with the schema, on this platform's test driver. */
internal expect fun testDriver(): SqlDriver

/** The first column of every row that [sql] returns, as text. */
internal fun SqlDriver.strings(sql: String): List<String> =
    executeQuery(
        identifier = null,
        sql = sql,
        mapper = { cursor ->
            QueryResult.Value(buildList { while (cursor.next().value) add(cursor.getString(0).orEmpty()) })
        },
        parameters = 0,
    ).value
