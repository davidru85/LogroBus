package com.ruizurraca.logrobus.feature.lines.presentation

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.transit.domain.Line
import com.ruizurraca.logrobus.transit.domain.LineKind
import kotlin.test.Test
import kotlin.test.assertEquals

/** LB-101 (FR-LINE-01, FR-LINE-02): the Lines list's state, from the lines or the failure to get them. */
class LinesUiMapperTest {
    @Test
    fun lb101_dayLinesComeFirst_thenNightLines_eachSortedByCode() {
        // The network of lines_2026-10-22.json, shuffled: there is no line 8.
        val codes = listOf("10", "B2", "3", "1", "11", "B1", "9", "4", "7", "B3", "2", "6", "5")

        val state = LinesUiMapper.state(AppResult.Success(codes.map(::line)))

        assertEquals(
            LinesUiState.Content(
                listOf(
                    LineGroupUi(LineKind.DAY, listOf("1", "2", "3", "4", "5", "6", "7", "9", "10", "11").map(::row)),
                    LineGroupUi(LineKind.NIGHT, listOf("B1", "B2", "B3").map(::row)),
                ),
            ),
            state,
        )
    }

    @Test
    fun lb101_codesSortByTheirNumber_notAsText() {
        val state = LinesUiMapper.state(AppResult.Success(listOf("B10", "B9", "100", "20").map(::line)))

        assertEquals(
            listOf(listOf("20", "100"), listOf("B9", "B10")),
            (state as LinesUiState.Content).groups.map { group -> group.lines.map { it.code } },
        )
    }

    @Test
    fun lb101_aRowHasTheBadgeColoursTheDisplayNameAndWhetherTheLineRuns() {
        val stopped =
            Line(
                id = 3,
                code = "3",
                displayName = "Campillo - El Cubo",
                color = 0xFF0FDCFC,
                onColor = 0xFF000000,
                kind = LineKind.DAY,
                isOperational = false,
            )

        val state = LinesUiMapper.state(AppResult.Success(listOf(stopped)))

        assertEquals(
            LineRowUi(
                id = 3,
                code = "3",
                displayName = "Campillo - El Cubo",
                color = 0xFF0FDCFC,
                onColor = 0xFF000000,
                isOperational = false,
            ),
            (state as LinesUiState.Content)
                .groups
                .single()
                .lines
                .single(),
        )
    }

    @Test
    fun lb101_aKindWithNoLinesHasNoGroup() {
        val state = LinesUiMapper.state(AppResult.Success(listOf(line("B1"))))

        assertEquals(listOf(LineKind.NIGHT), (state as LinesUiState.Content).groups.map { it.kind })
    }

    @Test
    fun lb101_noLinesIsTheEmptyState() {
        assertEquals(LinesUiState.Empty, LinesUiMapper.state(AppResult.Success(emptyList())))
    }

    @Test
    fun lb101_aFailureIsTheErrorStateWithItsReason() {
        assertEquals(
            LinesUiState.Error(ErrorReason.NETWORK),
            LinesUiMapper.state(AppResult.Failure(AppError.Network)),
        )
        assertEquals(LinesUiState.Error(ErrorReason.DATA), LinesUiMapper.state(AppResult.Failure(AppError.Data)))
    }

    private fun line(code: String) =
        Line(
            id = idOf(code),
            code = code,
            displayName = "Line $code",
            color = 0xFFE30613,
            onColor = 0xFFFFFFFF,
            kind = if (code.startsWith("B")) LineKind.NIGHT else LineKind.DAY,
            isOperational = true,
        )

    private fun row(code: String) =
        LineRowUi(
            id = idOf(code),
            code = code,
            displayName = "Line $code",
            color = 0xFFE30613,
            onColor = 0xFFFFFFFF,
            isOperational = true,
        )

    private fun idOf(code: String) = code.filter(Char::isDigit).toInt() + if (code.startsWith("B")) 1_000 else 0
}
