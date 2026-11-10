package com.ruizurraca.logrobus.feature.lines.domain

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.transit.domain.Arrival
import com.ruizurraca.logrobus.transit.domain.ArrivalsRepository
import com.ruizurraca.logrobus.transit.domain.Direction
import kotlinx.coroutines.flow.Flow

/**
 * FR-LINE-05: the calls of line [lineId], polled while its detail shows them, or the failure to get them. A
 * [direction] of `null` asks for every call of the line, which is what a caller with no direction in mind needs. An
 * interface, because Swift calls it and Swift tests replace it (architecture.md §4.6).
 *
 * It filters nothing: the repository keeps the calls of the line and the direction asked for, since guessing a
 * direction would put a bus on the wrong pattern.
 */
fun interface ObserveLineArrivals {
    operator fun invoke(
        lineId: Int,
        direction: Direction?,
    ): Flow<AppResult<List<Arrival>>>
}

/**
 * The repository's calls, as they come (LB-105). A use case of its own rather than the repository itself, so that
 * Swift reaches the arrivals through the same door as the lines (architecture.md §5.2), and the polling stops when the
 * flow stops being collected.
 */
class DefaultObserveLineArrivals(
    private val repository: ArrivalsRepository,
) : ObserveLineArrivals {
    override fun invoke(
        lineId: Int,
        direction: Direction?,
    ): Flow<AppResult<List<Arrival>>> = repository.observeLineArrivals(lineId, direction)
}
