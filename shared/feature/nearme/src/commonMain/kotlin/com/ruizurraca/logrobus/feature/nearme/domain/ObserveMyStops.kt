package com.ruizurraca.logrobus.feature.nearme.domain

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.presentation.ServedArrivalUi
import com.ruizurraca.logrobus.favourites.domain.FavouriteAddition
import com.ruizurraca.logrobus.favourites.domain.SavedStop
import com.ruizurraca.logrobus.favourites.domain.SavedStopRepository
import com.ruizurraca.logrobus.transit.domain.Stop
import com.ruizurraca.logrobus.transit.domain.StopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
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

/**
 * The saved stops, the name each one shows (FR-STOP-08), and the calls at each (FR-HOME-02, LB-204). The calls arrive
 * already worded — the row type is `core:presentation`'s, which both screens may use, and a feature may not depend on
 * another feature (ADR-0007) — so this module only decides how many a row shows.
 */
data class MyStops(
    val stops: List<SavedStop>,
    val names: Map<Int, String>,
    /** LB-204: each saved stop's calls, by stop id; a stop with no answer yet has no entry. */
    val arrivals: Map<Int, List<ServedArrivalUi>> = emptyMap(),
    /** FR-ERR-03: the stops whose own read failed, so their rows keep what they last showed. */
    val failed: Set<Int> = emptySet(),
)

class DefaultObserveMyStops(
    private val saved: SavedStopRepository,
    private val stops: StopRepository,
    /**
     * LB-204: the calls at one stop, already worded for a row. Passed in, not reached for: the stop detail owns that
     * read and this module may not depend on it, so the graph supplies it and a preview scripts it.
     */
    private val arrivalsOf: (stopId: Int) -> Flow<AppResult<List<ServedArrivalUi>>> = { emptyFlow() },
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
                    val answers = rows.value.map { stop -> stop.stopId to arrivalsOf(stop.stopId).firstOrNull() }
                    AppResult.Success(
                        MyStops(
                            stops = rows.value,
                            names = names,
                            arrivals =
                                answers
                                    .mapNotNull { (stopId, answer) ->
                                        (answer as? AppResult.Success)?.let { stopId to it.value }
                                    }.toMap(),
                            failed =
                                answers
                                    .filter { (_, answer) ->
                                        answer is AppResult.Failure
                                    }.map { it.first }
                                    .toSet(),
                        ),
                    )
                }
            }
        }
}

/** The first value of a flow, or a data failure when it ends without one. */
private suspend fun Flow<AppResult<Stop>>.firstValue(): AppResult<Stop> =
    firstOrNull() ?: AppResult.Failure(AppError.Data)
