package com.ruizurraca.logrobus.transit.domain

/**
 * One stop of the network (spec §8.8), with the lines whose patterns call at it (FR-STOP-03).
 *
 * [servedLines] is read from the cached catalogue, so a stop detail shows its header before the live source
 * answers. A stop no line reaches keeps none, which the screen says rather than hides. [isProvisional] is DD-10's
 * synthesised stop, whose name the source only gave through a pattern.
 */
data class Stop(
    val id: Int,
    val name: String,
    val lat: Double,
    val lng: Double,
    val isOperational: Boolean,
    val isProvisional: Boolean,
    val servedLines: List<ServedLine> = emptyList(),
)

/**
 * A line that calls at a stop (FR-STOP-03, DD-16): its id, its code as the badge shows it, and the [headsign] of the
 * pattern that reaches the stop.
 *
 * [direction] is the pattern's direction, so an arrival can take the headsign of the journey it belongs to rather than
 * another's. It is `null` for a line whose pattern this version cannot place.
 */
data class ServedLine(
    val lineId: Int,
    val code: String,
    val headsign: String?,
    val direction: Direction?,
    val color: Long,
    val onColor: Long,
)

/** FR-STOP-03: one stop of the cached catalogue, or the failure that its data is not there. */
fun interface StopRepository {
    fun observeStop(stopId: Int): kotlinx.coroutines.flow.Flow<com.ruizurraca.logrobus.core.common.AppResult<Stop>>
}
