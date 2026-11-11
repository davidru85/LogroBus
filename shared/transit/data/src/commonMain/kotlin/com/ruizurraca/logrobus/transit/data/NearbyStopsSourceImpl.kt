package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.geo.BoundingBox
import com.ruizurraca.logrobus.core.common.warn
import com.ruizurraca.logrobus.core.database.LogroBusDatabase
import com.ruizurraca.logrobus.transit.domain.NearbyStopRow
import com.ruizurraca.logrobus.transit.domain.NearbyStopsSource

private const val TAG = "NearbyStops"

/**
 * FR-HOME-03 and FR-STOP-11: the stops of the cached catalogue inside a bounding box, read in one query
 * (architecture.md §4.4). The box keeps the read small; the caller keeps the stops within its own radius and sorts
 * them, because sqlite has no Haversine and the exact distance is the caller's to compute.
 *
 * The database boundary turns a failure into a value, never a crash (code_style.md §2.2).
 */
class NearbyStopsSourceImpl(
    private val database: LogroBusDatabase,
    private val logger: Logger,
) : NearbyStopsSource {
    override fun stopsIn(box: BoundingBox): AppResult<List<NearbyStopRow>> =
        try {
            AppResult.Success(
                database.stopQueries
                    .selectStopsInBox(
                        south = box.south,
                        north = box.north,
                        west = box.west,
                        east = box.east,
                    ).executeAsList()
                    .map { row ->
                        NearbyStopRow(
                            stopId = row.id.toInt(),
                            name = row.name,
                            lat = row.lat,
                            lng = row.lng,
                        )
                    },
            )
        } catch (failure: RuntimeException) {
            logger.warn(TAG, "Reading the stops near a position failed")
            AppResult.Failure(AppError.Data)
        }
}
