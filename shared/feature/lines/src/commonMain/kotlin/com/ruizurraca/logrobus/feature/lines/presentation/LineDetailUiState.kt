package com.ruizurraca.logrobus.feature.lines.presentation

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.feature.lines.domain.LinesWithFavourites
import com.ruizurraca.logrobus.transit.domain.Direction
import com.ruizurraca.logrobus.transit.domain.Line

/**
 * A line's detail (SC-04), in the UX states of spec §7.3: the line, its favourite toggle (LB-103, FR-LINE-09), the
 * direction toggle and the ordered stop list of the selected direction (LB-104, FR-LINE-04). The map of both shapes
 * waits for LB-013, whose module holds no source yet and whose tile provider is OQ-10 (owner decision, 2026-11-10).
 */
sealed interface LineDetailUiState {
    data object Loading : LineDetailUiState

    data class Content(
        val line: LineDetailUi,
        /** FR-LINE-04: the direction whose stops the list shows. */
        val direction: Direction,
        /** Both directions, each labelled for the toggle. Empty only for a line with no patterns. */
        val directions: List<DirectionOptionUi>,
        /** The selected direction's stops, in sequence order. */
        val stops: List<StopRowUi>,
    ) : LineDetailUiState

    /** FR-ERR-05: the message for [reason], and Retry. */
    data class Error(
        val reason: ErrorReason,
    ) : LineDetailUiState
}

/** What the line detail sends its ViewModel. Going back is navigation, which the app shell does. */
sealed interface LineDetailEvent {
    /** FR-ERR-05: load again after an Error state. */
    data object Retry : LineDetailEvent

    /** FR-LINE-09: the toolbar's favourite toggle, with the value it now shows. */
    data class SetFavourite(
        val isFavourite: Boolean,
    ) : LineDetailEvent

    /** LB-110 (FR-FAV-05): the limit message has been shown and dismissed, so it does not show again. */
    data object FavouriteLimitShown : LineDetailEvent

    /** FR-LINE-04: the direction toggle picked the other direction. */
    data class SelectDirection(
        val direction: Direction,
    ) : LineDetailEvent
}

/**
 * One option of the direction toggle (FR-LINE-04): the direction it selects and its headsign, the last stop of that
 * direction's pattern (DD-16: the headsign is what tells two stops of the same name apart).
 */
data class DirectionOptionUi(
    val direction: Direction,
    val headsign: String,
    val isSelected: Boolean,
)

/** A stop in the detail's ordered list (FR-LINE-04): its position, who it is, and where (for LB-013's map). */
data class StopRowUi(
    val stopId: Int,
    val order: Int,
    val name: String,
    val lat: Double,
    val lng: Double,
)

/** The line's badge ([code] on [color] in [onColor], ARGB), its display name, and whether it is a favourite. */
data class LineDetailUi(
    val id: Int,
    val code: String,
    val displayName: String,
    val color: Long,
    val onColor: Long,
    val isFavourite: Boolean,
)

object LineDetailUiMapper {
    /**
     * FR-LINE-04: [direction] is the one the toggle has selected, or `null` for the line's first, which is the
     * outbound when it has one. A direction the line does not run falls back to that first, so a stale selection
     * after a catalogue change shows a pattern that exists rather than an empty list.
     */
    fun state(
        result: AppResult<LinesWithFavourites>,
        lineId: Int,
        direction: Direction? = null,
    ): LineDetailUiState =
        when (result) {
            is AppResult.Success -> {
                // Only a stale id finds no line: the detail opens from the lines that are shown.
                result.value.lines
                    .firstOrNull { it.id == lineId }
                    ?.let { line ->
                        val selected = direction?.takeIf { it in line.patterns } ?: line.firstDirection()
                        LineDetailUiState.Content(
                            line =
                                LineDetailUi(
                                    id = line.id,
                                    code = line.code,
                                    displayName = line.displayName,
                                    color = line.color,
                                    onColor = line.onColor,
                                    isFavourite = line.id in result.value.favouriteIds,
                                ),
                            direction = selected ?: Direction.OUTBOUND,
                            directions =
                                line.patterns.entries
                                    .sortedBy { it.key.ordinal }
                                    .map { (option, pattern) ->
                                        DirectionOptionUi(option, pattern.headsign, option == selected)
                                    },
                            stops =
                                selected?.let { line.patterns[it] }?.stops.orEmpty().mapIndexed { index, stop ->
                                    StopRowUi(
                                        stopId = stop.stopId,
                                        order = index + 1,
                                        name = stop.name,
                                        lat = stop.lat,
                                        lng = stop.lng,
                                    )
                                },
                        )
                    } ?: LineDetailUiState.Error(ErrorReason.DATA)
            }

            is AppResult.Failure -> {
                LineDetailUiState.Error(ErrorReason.of(result.error))
            }
        }
}

/** The direction whose stops the detail shows by default: the outbound, or whichever it has (FR-LINE-04). */
private fun Line.firstDirection(): Direction? = patterns.keys.minByOrNull { it.ordinal }
