package com.ruizurraca.logrobus.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.ruizurraca.logrobus.core.common.text.foldForSearch

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

/** Inserts a stop with plain SQL, as a sync would, its name folded as the normaliser folds it. */
internal fun SqlDriver.insertStop(
    id: Long,
    name: String,
) {
    execute(
        identifier = null,
        sql =
            "INSERT INTO stop (id, name, lat, lng, operational, provisional, normalized_name) " +
                "VALUES (?, ?, 42.4650, -2.4456, 1, 0, ?)",
        parameters = 3,
    ) {
        bindLong(0, id)
        bindString(1, name)
        bindString(2, foldForSearch(name))
    }
}
