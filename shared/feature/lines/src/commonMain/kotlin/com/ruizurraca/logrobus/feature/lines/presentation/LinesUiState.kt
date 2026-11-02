package com.ruizurraca.logrobus.feature.lines.presentation

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.transit.domain.Line
import com.ruizurraca.logrobus.transit.domain.LineKind

/** The Lines list (LB-101, FR-LINE-01, FR-LINE-02), in the UX states of spec §7.3. */
sealed interface LinesUiState {
    data object Loading : LinesUiState

    /** Day lines, then night (Búho) lines; a kind with no lines has no group. */
    data class Content(
        val groups: List<LineGroupUi>,
    ) : LinesUiState

    /** No lines at all: one sentence and Retry (spec §7.3). */
    data object Empty : LinesUiState

    /** FR-ERR-05: the message for [reason], and Retry. */
    data class Error(
        val reason: ErrorReason,
    ) : LinesUiState
}

/** What the Lines screen sends its ViewModel. */
sealed interface LinesEvent {
    /** FR-ERR-05: load again after an Error or Empty state. */
    data object Retry : LinesEvent
}

/** The lines of one [kind], sorted by code. Each platform words the group's title. */
data class LineGroupUi(
    val kind: LineKind,
    val lines: List<LineRowUi>,
)

/**
 * A row of the list (FR-LINE-02): the badge's [code] on [color] in [onColor] (ARGB), the display name, and a status chip
 * when the line is not operational. The badge's spoken label names the code, never the colour (NFR-A11Y-02).
 */
data class LineRowUi(
    val id: Int,
    val code: String,
    val displayName: String,
    val color: Long,
    val onColor: Long,
    val isOperational: Boolean,
)

object LinesUiMapper {
    fun state(result: AppResult<List<Line>>): LinesUiState =
        when (result) {
            is AppResult.Success -> {
                if (result.value.isEmpty()) LinesUiState.Empty else LinesUiState.Content(groupsOf(result.value))
            }

            is AppResult.Failure -> {
                LinesUiState.Error(ErrorReason.of(result.error))
            }
        }

    // FR-LINE-01: the kinds in their order (day, then night), each sorted by the number in its code: 9 before 10.
    private fun groupsOf(lines: List<Line>): List<LineGroupUi> =
        LineKind.entries.mapNotNull { kind ->
            lines
                .filter { it.kind == kind }
                .sortedWith(compareBy<Line> { numberOf(it.code) }.thenBy { it.code })
                .takeIf { it.isNotEmpty() }
                ?.let { group -> LineGroupUi(kind, group.map(::rowOf)) }
        }

    // A code is "B" and up to three digits (spec §5.5), so its digits always make a number.
    private fun numberOf(code: String): Int = code.filter(Char::isDigit).toIntOrNull() ?: Int.MAX_VALUE

    private fun rowOf(line: Line) =
        LineRowUi(
            id = line.id,
            code = line.code,
            displayName = line.displayName,
            color = line.color,
            onColor = line.onColor,
            isOperational = line.isOperational,
        )
}
