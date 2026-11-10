package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.common.AppDispatchers
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.warn
import com.ruizurraca.logrobus.core.database.LogroBusDatabase
import com.ruizurraca.logrobus.core.database.SelectStopWithServedLines
import com.ruizurraca.logrobus.transit.domain.Direction
import com.ruizurraca.logrobus.transit.domain.ServedLine
import com.ruizurraca.logrobus.transit.domain.Stop
import com.ruizurraca.logrobus.transit.domain.StopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

private const val TAG = "Stops"

/**
 * FR-STOP-01 and FR-STOP-03: one stop of the cached catalogue with the lines that serve it, read from the database
 * (architecture.md §4.4) so the detail shows at once, without a source.
 *
 * The served lines carry the badge colours the catalogue stores and the headsigns of the patterns that pass the stop
 * (DD-16), which is what "towards where" means for this stop. The database boundary turns a failure into a value, never
 * a crash (code_style.md §2.2).
 */
class StopRepositoryImpl(
    private val database: LogroBusDatabase,
    private val dispatchers: AppDispatchers,
    private val logger: Logger,
) : StopRepository {
    override fun observeStop(stopId: Int): Flow<AppResult<Stop>> =
        flow<List<SelectStopWithServedLines>> {
            emit(database.stopQueries.selectStopWithServedLines(stopId.toLong()).executeAsList())
        }.catch { failure ->
            logger.warn(TAG, "Reading a stop failed", failure)
            emit(emptyList())
        }.map { rows -> stopOf(stopId, rows) }

    /**
     * The stop, or a data failure when the catalogue does not hold it. A stop the sync has not stored yet is not an
     * error of the source: the screen says its data is missing and offers Retry, which re-reads the cache.
     */
    private fun stopOf(
        stopId: Int,
        rows: List<SelectStopWithServedLines>,
    ): AppResult<Stop> {
        val first = rows.firstOrNull()
        // A stop with no serving line still comes back as one row of nulls, because the query is a LEFT JOIN.
        val name = first?.name ?: return AppResult.Failure(AppError.Data)
        val lines =
            rows.mapNotNull { row ->
                val lineId = row.line_id ?: return@mapNotNull null
                val code = row.code ?: return@mapNotNull null
                ServedLine(
                    lineId = lineId.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt() ?: return@mapNotNull null,
                    code = code,
                    headsign = row.headsign,
                    color = row.color ?: 0,
                    onColor = row.on_color ?: 0,
                    direction = Direction.entries.firstOrNull { it.key == row.direction },
                )
            }.distinctBy { it.lineId to it.headsign }
        return AppResult.Success(
            Stop(
                id = stopId,
                name = name,
                lat = first.lat,
                lng = first.lng,
                isOperational = first.operational,
                isProvisional = first.provisional,
                servedLines = lines,
            ),
        )
    }
}
