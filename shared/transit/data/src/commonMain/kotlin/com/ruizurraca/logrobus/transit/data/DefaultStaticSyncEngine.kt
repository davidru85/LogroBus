package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.common.AppDispatchers
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.database.LogroBusDatabase
import com.ruizurraca.logrobus.core.database.searchableName
import com.ruizurraca.logrobus.transit.domain.StaticDataset
import com.ruizurraca.logrobus.transit.domain.StaticSyncEngine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement

/** The static sync of architecture.md §4.4. */
@Suppress("UnusedPrivateProperty") // logger: a stub until its green commit.
internal class DefaultStaticSyncEngine(
    private val source: StaticSource,
    private val normaliser: StaticNormaliser,
    private val database: LogroBusDatabase,
    private val clock: AppClock,
    private val dispatchers: AppDispatchers,
    private val logger: Logger,
) : StaticSyncEngine {
    override suspend fun syncIfStale(): AppResult<Unit> =
        source.lines().then { lines ->
            source.stops().then { stops ->
                normaliser.normalise(lines, stops).then { catalogue -> store(catalogue, lines, stops) }
            }
        }

    private suspend fun store(
        catalogue: StaticCatalogue,
        lines: JsonElement,
        stops: JsonElement,
    ): AppResult<Unit> {
        val hashes =
            mapOf(
                StaticDataset.LINES to sha256Hex(lines.toString()),
                StaticDataset.STOPS to sha256Hex(stops.toString()),
            )
        val now = clock.now()
        withContext(dispatchers.io) {
            database.transaction {
                replace(catalogue)
                hashes.forEach { (dataset, hash) -> database.syncQueries.recordSync(dataset.key, now, hash) }
            }
        }
        return AppResult.Success(Unit)
    }

    private fun replace(catalogue: StaticCatalogue) {
        with(database.lineQueries) {
            deletePatternStops()
            deletePatterns()
            deleteLines()
            catalogue.lines.forEach { line ->
                insertLine(
                    id = line.id.toLong(),
                    code = line.code,
                    raw_name = line.rawName,
                    display_name = line.displayName,
                    color = line.color,
                    on_color = line.onColor,
                    kind = line.kind.key,
                    operational = line.isOperational,
                )
                line.patterns.forEach { pattern ->
                    insertPattern(line.id.toLong(), pattern.direction.key, pattern.headsign, pattern.shapePolyline)
                    pattern.stops.forEachIndexed { seq, stop ->
                        insertPatternStop(
                            line_id = line.id.toLong(),
                            direction = pattern.direction.key,
                            seq = seq.toLong(),
                            stop_id = stop.stopId.toLong(),
                            shape_index = stop.shapeIndex.toLong(),
                        )
                    }
                }
            }
        }
        with(database.stopQueries) {
            deleteStopLines()
            deleteStops()
            catalogue.stops.forEach { stop ->
                insertStop(
                    id = stop.id.toLong(),
                    name = stop.name,
                    lat = stop.lat,
                    lng = stop.lng,
                    operational = stop.isOperational,
                    provisional = stop.isProvisional,
                    normalized_name = searchableName(stop.name),
                )
                stop.lineIds.forEach { lineId -> insertStopLine(stop.id.toLong(), lineId.toLong()) }
            }
        }
    }
}

/** [next] on a success's value; a failure passes through. */
private inline fun <T, R> AppResult<T>.then(next: (T) -> AppResult<R>): AppResult<R> =
    when (this) {
        is AppResult.Success -> next(value)
        is AppResult.Failure -> this
    }
