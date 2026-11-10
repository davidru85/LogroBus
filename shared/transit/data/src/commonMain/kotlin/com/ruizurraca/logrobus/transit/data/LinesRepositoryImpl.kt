package com.ruizurraca.logrobus.transit.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.ruizurraca.logrobus.core.common.AppDispatchers
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.warn
import com.ruizurraca.logrobus.core.database.LogroBusDatabase
import com.ruizurraca.logrobus.core.database.SelectLines
import com.ruizurraca.logrobus.transit.domain.Direction
import com.ruizurraca.logrobus.transit.domain.Line
import com.ruizurraca.logrobus.transit.domain.LineKind
import com.ruizurraca.logrobus.transit.domain.LinesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Reads the lines that the static sync stores (architecture.md §4.4). */
class LinesRepositoryImpl(
    private val database: LogroBusDatabase,
    private val dispatchers: AppDispatchers,
    private val logger: Logger,
) : LinesRepository {
    override fun observeLines(): Flow<AppResult<List<Line>>> =
        combine(
            database.lineQueries
                .selectLines()
                .asFlow()
                .mapToList(dispatchers.io),
            // LB-102 part 2: the names of the stops each line serves, folded for the search. Read with the lines,
            // in the same emission, so a line never shows without them.
            database.lineQueries
                .selectLineStopNames()
                .asFlow()
                .mapToList(dispatchers.io),
        ) { rows, stopNames ->
            val namesByLine = stopNames.groupBy({ it.line_id }) { it.normalized_name }
            // One row per pattern: a line's rows are next to each other, in id order.
            rows.groupBy { it.id }.values.mapNotNull { line -> lineOf(line, namesByLine) }
        }.map<_, AppResult<List<Line>>> { AppResult.Success(it) }
            // The database boundary (code_style.md §2.2): a failure ends the flow as a value, never as a crash.
            .catch { failure ->
                logger.warn(TAG, "Reading the lines failed", failure)
                emit(AppResult.Failure(AppError.Data))
            }

    private fun lineOf(
        rows: List<SelectLines>,
        stopNames: Map<Long, List<String>>,
    ): Line? {
        val row = rows.first()
        val kind = LineKind.entries.firstOrNull { it.key == row.kind }
        // DD-02: ids are Ints. A wider one would be truncated into another line's id.
        val id = row.id.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt()
        if (kind == null || id == null) {
            // By cause only, never with the row's values.
            val cause = if (kind == null) "an unknown kind" else "an id out of range"
            logger.warn(TAG, "Dropped a line with $cause")
            return null
        }
        return Line(
            id = id,
            code = row.code,
            displayName = row.display_name,
            color = row.color,
            onColor = row.on_color,
            kind = kind,
            isOperational = row.operational,
            headsigns = headsignsOf(rows),
            stopNames = stopNames[id.toLong()].orEmpty(),
        )
    }

    // FR-LINE-13: outbound first. A pattern of a direction this version does not know is left out.
    private fun headsignsOf(rows: List<SelectLines>): List<String> =
        rows
            .mapNotNull { row -> Direction.entries.firstOrNull { it.key == row.direction }?.to(row.headsign) }
            .sortedBy { (direction, _) -> direction.ordinal }
            .mapNotNull { (_, headsign) -> headsign }

    private companion object {
        const val TAG = "Lines"
    }
}
