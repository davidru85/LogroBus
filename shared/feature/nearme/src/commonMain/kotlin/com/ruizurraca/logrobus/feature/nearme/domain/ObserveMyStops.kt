package com.ruizurraca.logrobus.feature.nearme.domain

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.favourites.domain.FavouriteAddition
import com.ruizurraca.logrobus.favourites.domain.SavedStop
import com.ruizurraca.logrobus.favourites.domain.SavedStopRepository
import com.ruizurraca.logrobus.transit.domain.Stop
import com.ruizurraca.logrobus.transit.domain.StopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/**
 * FR-HOME-02 and FR-STOP-08: "My stops", the saved stops with the name each row shows.
 *
 * A saved stop's alias wins; without one the catalogue's name shows, so a stop the user saved under its own name reads
 * as the rider knows it. The name is read from the cached catalogue, which is why this joins the two.
 */
fun interface ObserveMyStops {
    operator fun invoke(): Flow<AppResult<MyStops>>
}

/** The saved stops and the name each one shows (FR-STOP-08). */
data class MyStops(
    val stops: List<SavedStop>,
    val names: Map<Int, String>,
)

class DefaultObserveMyStops(
    private val saved: SavedStopRepository,
    private val stops: StopRepository,
) : ObserveMyStops {
    override fun invoke(): Flow<AppResult<MyStops>> =
        saved.observeSavedStops().map { rows ->
            when (rows) {
                is AppResult.Failure -> {
                    rows
                }

                is AppResult.Success -> {
                    // The names are read once per change of the saved list, not collected: the catalogue's own flow
                    // for a stop emits its current row and finishes.
                    val names =
                        rows.value
                            .mapNotNull { stop ->
                                when (val read = stops.observeStop(stop.stopId).firstValue()) {
                                    is AppResult.Success -> stop.stopId to read.value.name
                                    is AppResult.Failure -> null
                                }
                            }.toMap()
                    AppResult.Success(MyStops(stops = rows.value, names = names))
                }
            }
        }
}

/** The first value of a flow, or a data failure when it ends without one. */
private suspend fun Flow<AppResult<Stop>>.firstValue(): AppResult<Stop> =
    firstOrNull() ?: AppResult.Failure(AppError.Data)
