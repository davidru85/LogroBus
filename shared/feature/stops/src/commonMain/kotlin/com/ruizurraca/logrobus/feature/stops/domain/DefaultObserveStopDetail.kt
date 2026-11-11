package com.ruizurraca.logrobus.feature.stops.domain

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.transit.domain.Stop
import com.ruizurraca.logrobus.transit.domain.StopRepository
import kotlinx.coroutines.flow.Flow

/**
 * FR-STOP-03: one stop of the cached catalogue with the lines that serve it, for the detail's header. A use case of its
 * own rather than the repository itself, so Swift reaches the stop through the same door as the lines (architecture.md
 * §5.2), and a preview or a test replaces it without a database.
 */
class DefaultObserveStopDetail(
    private val repository: StopRepository,
) : ObserveStopDetail {
    override fun invoke(stopId: Int): Flow<AppResult<Stop>> = repository.observeStop(stopId)
}
