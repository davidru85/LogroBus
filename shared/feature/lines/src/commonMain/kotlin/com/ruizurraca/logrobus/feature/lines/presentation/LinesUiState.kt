package com.ruizurraca.logrobus.feature.lines.presentation

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.feature.lines.domain.LineQuery
import com.ruizurraca.logrobus.feature.lines.domain.LinesWithFavourites
import com.ruizurraca.logrobus.transit.domain.Line
import com.ruizurraca.logrobus.transit.domain.LineKind

/**
 * The Lines tab (LB-101, FR-LINE-01, FR-LINE-02; LB-103, FR-LINE-12 to 15; LB-102, FR-LINE-03, FR-LINE-16), in the UX
 * states of spec §7.3.
 */
sealed interface LinesUiState {
    data object Loading : LinesUiState

    /**
     * The "Your favourite lines" carousel, then day lines and night (Búho) lines; a kind with no lines has no group.
     * With no [favourites], the carousel shows its hint card instead (FR-LINE-14).
     */
    data class Content(
        val favourites: List<FavouriteLineCardUi>,
        val groups: List<LineGroupUi>,
    ) : LinesUiState

    /** FR-LINE-16: the lines a search found, in the list's groups, without the favourites carousel. */
    data class SearchResults(
        val groups: List<LineGroupUi>,
    ) : LinesUiState

    /** A search that found no line: one sentence and one action, "Clear search" (spec §7.3). */
    data object NoMatches : LinesUiState

    /** No lines at all: one sentence and Retry (spec §7.3). */
    data object Empty : LinesUiState

    /** FR-ERR-05: the message for [reason], and Retry. */
    data class Error(
        val reason: ErrorReason,
    ) : LinesUiState
}

/** What the Lines tab sends its ViewModel. Opening a line is navigation, which the app shell does. */
sealed interface LinesEvent {
    /** FR-ERR-05: load again after an Error or Empty state. */
    data object Retry : LinesEvent

    /** FR-LINE-15: a row's favourite toggle, with the value it now shows. */
    data class SetFavourite(
        val lineId: Int,
        val isFavourite: Boolean,
    ) : LinesEvent
}

/** The lines of one [kind], sorted by code. Each platform words the group's title. */
data class LineGroupUi(
    val kind: LineKind,
    val lines: List<LineRowUi>,
)

/**
 * A row of the list (FR-LINE-02): the badge's [code] on [color] in [onColor] (ARGB), the display name, a status chip
 * when the line is not operational, and the favourite toggle (FR-LINE-15). The badge's spoken label names the code,
 * never the colour (NFR-A11Y-02).
 */
data class LineRowUi(
    val id: Int,
    val code: String,
    val displayName: String,
    val color: Long,
    val onColor: Long,
    val isOperational: Boolean,
    val isFavourite: Boolean,
)

/**
 * A card of the favourite lines carousel (FR-LINE-13): the badge, the display name in at most two lines, and the
 * [headsigns] of both directions, outbound first. Tapping it opens the line's detail.
 */
data class FavouriteLineCardUi(
    val id: Int,
    val code: String,
    val displayName: String,
    val color: Long,
    val onColor: Long,
    val headsigns: List<String>,
)

object LinesUiMapper {
    /** The state of [result] for the search field's [query]; with no query, the whole list and its favourites. */
    fun state(
        result: AppResult<LinesWithFavourites>,
        query: String = "",
    ): LinesUiState =
        when (result) {
            is AppResult.Success -> {
                val lines = result.value.lines
                val search = LineQuery(query)
                when {
                    lines.isEmpty() -> LinesUiState.Empty
                    search.isActive -> searchResults(lines.filter(search::matches), result.value.favouriteIds)
                    else -> content(lines, result.value.favouriteIds)
                }
            }

            is AppResult.Failure -> {
                LinesUiState.Error(ErrorReason.of(result.error))
            }
        }

    private fun content(
        lines: List<Line>,
        favouriteIds: List<Int>,
    ): LinesUiState.Content {
        val byId = lines.associateBy { it.id }
        // A favourite whose line left the catalogue stays saved, without a card until the line comes back.
        val favourites = favouriteIds.mapNotNull { byId[it] }.map(::cardOf)
        return LinesUiState.Content(favourites, groupsOf(lines, favouriteIds.toSet()))
    }

    private fun searchResults(
        found: List<Line>,
        favouriteIds: List<Int>,
    ): LinesUiState =
        if (found.isEmpty()) {
            LinesUiState.NoMatches
        } else {
            LinesUiState.SearchResults(groupsOf(found, favouriteIds.toSet()))
        }

    // FR-LINE-01: the kinds in their order (day, then night), each sorted by the number in its code: 9 before 10.
    private fun groupsOf(
        lines: List<Line>,
        favouriteIds: Set<Int>,
    ): List<LineGroupUi> =
        LineKind.entries.mapNotNull { kind ->
            lines
                .filter { it.kind == kind }
                .sortedWith(compareBy<Line> { numberOf(it.code) }.thenBy { it.code })
                .takeIf { it.isNotEmpty() }
                ?.let { group -> LineGroupUi(kind, group.map { rowOf(it, it.id in favouriteIds) }) }
        }

    // A code is "B" and up to three digits (spec §5.5), so its digits always make a number.
    private fun numberOf(code: String): Int = code.filter(Char::isDigit).toIntOrNull() ?: Int.MAX_VALUE

    private fun rowOf(
        line: Line,
        isFavourite: Boolean,
    ) = LineRowUi(
        id = line.id,
        code = line.code,
        displayName = line.displayName,
        color = line.color,
        onColor = line.onColor,
        isOperational = line.isOperational,
        isFavourite = isFavourite,
    )

    private fun cardOf(line: Line) =
        FavouriteLineCardUi(
            id = line.id,
            code = line.code,
            displayName = line.displayName,
            color = line.color,
            onColor = line.onColor,
            headsigns = line.headsigns,
        )
}
