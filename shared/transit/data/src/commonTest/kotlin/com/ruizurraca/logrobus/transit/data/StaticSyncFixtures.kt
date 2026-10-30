package com.ruizurraca.logrobus.transit.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.transit.domain.Direction
import com.ruizurraca.logrobus.transit.domain.LineKind
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/** Spec §8.5: the fewest lines and stops a payload may have. */
internal const val FEWEST_LINES = 5
internal const val FEWEST_STOPS = 100

/** A body that stands for one version of a payload; the fake normaliser reads the version back. */
internal fun body(
    dataset: String,
    version: Int,
): JsonElement = JsonPrimitive("$dataset v$version")

/** The source, answering with [lines] and [stops], and counting the requests. */
internal class FakeStaticSource(
    var lines: AppResult<JsonElement> = AppResult.Success(body("lines", 1)),
    var stops: AppResult<JsonElement> = AppResult.Success(body("stops", 1)),
) : StaticSource {
    var requests = 0
        private set

    override suspend fun lines(): AppResult<JsonElement> {
        requests++
        return lines
    }

    override suspend fun stops(): AppResult<JsonElement> {
        requests++
        return stops
    }
}

/** Answers with [result], counting the calls. */
internal class FakeNormaliser(
    var result: AppResult<StaticCatalogue> = AppResult.Success(catalogue()),
) : StaticNormaliser {
    var calls = 0
        private set

    override fun normalise(
        lines: JsonElement,
        stops: JsonElement,
    ): AppResult<StaticCatalogue> {
        calls++
        return result
    }
}

/**
 * A catalogue of [lineCount] lines and [stopCount] stops, every value marked with [version]. Each stop is served by
 * one line, in both directions, and sorted as [storedCatalogue] reads them back.
 */
internal fun catalogue(
    version: Int = 1,
    lineCount: Int = FEWEST_LINES,
    stopCount: Int = FEWEST_STOPS,
): StaticCatalogue {
    val lineOf = { stopId: Int -> (stopId - 1) % lineCount + 1 }
    val stops =
        (1..stopCount).map { id ->
            CatalogueStop(
                id = id,
                name = "Avenida de Colón $id v$version",
                lat = 42.46 + id / 10_000.0,
                lng = -2.44 - id / 10_000.0,
                isOperational = id % 7 != 0,
                isProvisional = false,
                lineIds = listOf(lineOf(id)),
            )
        }
    val lines =
        (1..lineCount).map { id ->
            val served = stops.filter { lineOf(it.id) == id }.map { it.id }
            CatalogueLine(
                id = id,
                code = if (id % 2 == 0) "B$id" else "$id",
                rawName = "$id-RAW v$version",
                displayName = "Line $id v$version",
                color = 0xFF00_0000L + id,
                onColor = 0xFFFF_FFFFL,
                kind = if (id % 2 == 0) LineKind.NIGHT else LineKind.DAY,
                isOperational = true,
                patterns =
                    Direction.entries.map { direction ->
                        val ordered = if (direction == Direction.OUTBOUND) served else served.reversed()
                        CataloguePattern(
                            direction = direction,
                            headsign = "Avenida de Colón ${ordered.last()} v$version",
                            shapePolyline = "shape $id $direction v$version",
                            stops = ordered.mapIndexed { index, stopId -> CataloguePatternStop(stopId, index * 3) },
                        )
                    },
            )
        }
    return StaticCatalogue(lines, stops)
}

/** Everything the sync stored, read back with plain SQL, in [catalogue]'s order. */
internal fun SqlDriver.storedCatalogue(): StaticCatalogue {
    val patternStops =
        select("SELECT line_id, direction, stop_id, shape_index FROM pattern_stop ORDER BY line_id, direction, seq") {
            (it.int(0) to direction(it.getString(1)!!)) to CataloguePatternStop(it.int(2), it.int(3))
        }.groupBy({ it.first }, { it.second })
    val patterns =
        select("SELECT line_id, direction, headsign, shape_polyline FROM pattern") {
            val key = it.int(0) to direction(it.getString(1)!!)
            it.int(0) to CataloguePattern(key.second, it.getString(2)!!, it.getString(3)!!, patternStops[key].orEmpty())
        }.groupBy({ it.first }, { it.second })
    val lines =
        select("SELECT id, code, raw_name, display_name, color, on_color, kind, operational FROM line ORDER BY id") {
            CatalogueLine(
                id = it.int(0),
                code = it.getString(1)!!,
                rawName = it.getString(2)!!,
                displayName = it.getString(3)!!,
                color = it.getLong(4)!!,
                onColor = it.getLong(5)!!,
                kind = LineKind.entries.single { kind -> kind.key == it.getString(6) },
                isOperational = it.getBoolean(7)!!,
                patterns = patterns[it.int(0)].orEmpty().sortedBy { pattern -> pattern.direction },
            )
        }
    val stopLines =
        select("SELECT stop_id, line_id FROM stop_line ORDER BY stop_id, line_id") { it.int(0) to it.int(1) }
            .groupBy({ it.first }, { it.second })
    val stops =
        select("SELECT id, name, lat, lng, operational, provisional FROM stop ORDER BY id") {
            CatalogueStop(
                id = it.int(0),
                name = it.getString(1)!!,
                lat = it.getDouble(2)!!,
                lng = it.getDouble(3)!!,
                isOperational = it.getBoolean(4)!!,
                isProvisional = it.getBoolean(5)!!,
                lineIds = stopLines[it.int(0)].orEmpty(),
            )
        }
    return StaticCatalogue(lines, stops)
}

/** The rows of [sql], each read by [row]. */
internal fun <T> SqlDriver.select(
    sql: String,
    row: (SqlCursor) -> T,
): List<T> =
    executeQuery(
        identifier = null,
        sql = sql,
        mapper = { cursor -> QueryResult.Value(buildList { while (cursor.next().value) add(row(cursor)) }) },
        parameters = 0,
    ).value

private fun SqlCursor.int(index: Int): Int = getLong(index)!!.toInt()

private fun direction(key: String): Direction = Direction.entries.single { it.key == key }
