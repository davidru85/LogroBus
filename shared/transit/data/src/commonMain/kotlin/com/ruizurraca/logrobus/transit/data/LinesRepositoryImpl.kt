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
import com.ruizurraca.logrobus.transit.domain.Line
import com.ruizurraca.logrobus.transit.domain.LineKind
import com.ruizurraca.logrobus.transit.domain.LinesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Reads the lines that the static sync stores (architecture.md §4.4). */
class LinesRepositoryImpl(
    private val database: LogroBusDatabase,
    private val dispatchers: AppDispatchers,
    private val logger: Logger,
) : LinesRepository {
    override fun observeLines(): Flow<AppResult<List<Line>>> =
        database.lineQueries
            .selectLines()
            .asFlow()
            .mapToList(dispatchers.io)
            .map { rows -> rows.mapNotNull(::lineOf) }
            .map<_, AppResult<List<Line>>> { AppResult.Success(it) }
            // The database boundary (code_style.md §2.2): a failure ends the flow as a value, never as a crash.
            .catch { failure ->
                logger.warn(TAG, "Reading the lines failed", failure)
                emit(AppResult.Failure(AppError.Data))
            }

    private fun lineOf(row: SelectLines): Line? {
        val kind = LineKind.entries.firstOrNull { it.key == row.kind }
        if (kind == null) {
            logger.warn(TAG, "Dropped a line of an unknown kind")
            return null
        }
        return Line(
            id = row.id.toInt(),
            code = row.code,
            displayName = row.display_name,
            color = row.color,
            onColor = row.on_color,
            kind = kind,
            isOperational = row.operational,
        )
    }

    private companion object {
        const val TAG = "Lines"
    }
}
