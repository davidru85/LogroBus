package com.ruizurraca.logrobus.feature.lines.presentation

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.feature.lines.domain.LinesWithFavourites

/**
 * A line's detail (SC-04), in the UX states of spec §7.3. Until LB-104 adds its map, direction toggle and stops, it
 * shows the line and its favourite toggle (LB-103, FR-LINE-09; owner decision, 2026-11-02).
 */
sealed interface LineDetailUiState {
    data object Loading : LineDetailUiState

    data class Content(
        val line: LineDetailUi,
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
}

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
    fun state(
        result: AppResult<LinesWithFavourites>,
        lineId: Int,
    ): LineDetailUiState =
        when (result) {
            is AppResult.Success -> {
                // Only a stale id finds no line: the detail opens from the lines that are shown.
                result.value.lines
                    .firstOrNull { it.id == lineId }
                    ?.let { line ->
                        LineDetailUiState.Content(
                            LineDetailUi(
                                id = line.id,
                                code = line.code,
                                displayName = line.displayName,
                                color = line.color,
                                onColor = line.onColor,
                                isFavourite = line.id in result.value.favouriteIds,
                            ),
                        )
                    } ?: LineDetailUiState.Error(ErrorReason.DATA)
            }

            is AppResult.Failure -> {
                LineDetailUiState.Error(ErrorReason.of(result.error))
            }
        }
}
