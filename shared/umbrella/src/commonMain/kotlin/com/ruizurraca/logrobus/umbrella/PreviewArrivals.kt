package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.transit.domain.Arrival
import com.ruizurraca.logrobus.transit.domain.ArrivalsRepository
import com.ruizurraca.logrobus.transit.domain.Direction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * The line detail's calls for previews and tests (LB-105), scripted like [PreviewSettings] and [PreviewLocation]: the
 * real arrivals repository over a flow of calls the test sets, so a preview or a snapshot shows what
 * `arrivalsByStop` makes of them. None until [script] sets some. Keyed nowhere: like the real repository, a collector
 * sees the calls of the line and direction it asked for, so a screen that changes its direction gets that one's own.
 */
internal class PreviewArrivals {
    private val scripted = MutableStateFlow(emptyList<Arrival>())

    /** LB-105: how many collections have started, so a test sees that a new direction asks again. */
    private val starts = MutableStateFlow(0)

    val collections: Int
        get() = starts.value

    /** LB-105: the calls a preview's arrivals use case emits, replacing the last script. */
    fun script(calls: List<Arrival>) {
        scripted.value = calls
    }

    /**
     * The repository over the script. [failing] is read on every collection, so a preview that goes offline after a
     * screen has collected still ends the flow as the real one does, and the screen shows its error.
     */
    fun repository(failing: () -> Boolean): ArrivalsRepository =
        ArrivalsRepository { lineId, direction ->
            flow {
                if (failing()) {
                    emit(AppResult.Failure(AppError.Data))
                } else {
                    emitAll(
                        scripted.map { calls ->
                            val shown = calls.filter { it.lineId == lineId }
                            AppResult.Success(
                                shown.filter { direction == null || it.direction == direction },
                            )
                        },
                    )
                }
            }.onStart { starts.update { count -> count + 1 } }
        }

    companion object {
        /** LB-105: where the preview's clock stands, so a scripted call reads "in 4 min" rather than a past time. */
        val SAMPLE_SYNC: Instant = Instant.parse("2026-10-28T09:30:00Z")

        /**
         * LB-105: two of line 10's outbound calls, at the sample line's own first two stops, which is what the detail
         * maps. They are 4 and 7 min out from [SAMPLE_SYNC], and the second is DD-09's low-confidence one, so its "~"
         * shows.
         */
        val A_RIDE_AWAY =
            Arrival(
                lineId = 10,
                stopId = 10 * SampleLines.STOPS_PER_DIRECTION,
                direction = Direction.OUTBOUND,
                expectedAt = SAMPLE_SYNC + 4.minutes,
            )
        val A_ROUGHER_PREDICTION =
            Arrival(
                lineId = 10,
                stopId = 10 * SampleLines.STOPS_PER_DIRECTION + 1,
                direction = Direction.OUTBOUND,
                expectedAt = SAMPLE_SYNC + 7.minutes,
                isLowConfidence = true,
            )
    }
}
