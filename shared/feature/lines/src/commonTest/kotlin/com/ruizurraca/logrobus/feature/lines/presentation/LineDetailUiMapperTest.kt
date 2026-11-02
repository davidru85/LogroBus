package com.ruizurraca.logrobus.feature.lines.presentation

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.feature.lines.domain.LinesWithFavourites
import com.ruizurraca.logrobus.transit.domain.Line
import com.ruizurraca.logrobus.transit.domain.LineKind
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * LB-103 (FR-LINE-09): the line detail's state, until LB-104 adds its map, directions and stops: the line's badge, its
 * name and whether it is a favourite.
 */
class LineDetailUiMapperTest {
    private val line3 = line(3, "3", "Campillo - El Cubo")
    private val line10 = line(10, "10", "El Arco - Hospital San Pedro")

    @Test
    fun lb103_theDetailShowsTheLinesBadgeAndName_andWhetherItIsAFavourite() {
        val result = AppResult.Success(LinesWithFavourites(listOf(line3, line10), favouriteIds = listOf(10)))

        assertEquals(
            LineDetailUiState.Content(
                LineDetailUi(
                    id = 3,
                    code = "3",
                    displayName = "Campillo - El Cubo",
                    color = 0xFF0FDCFC,
                    onColor = 0xFF000000,
                    isFavourite = false,
                ),
            ),
            LineDetailUiMapper.state(result, lineId = 3),
        )
        val favourite = LineDetailUiMapper.state(result, lineId = 10) as LineDetailUiState.Content
        assertEquals(true, favourite.line.isFavourite)
    }

    @Test
    fun lb103_aLineThatIsNotInTheCatalogueIsTheDataError() {
        // Only a stale id can ask for it: the detail opens from the lines that are shown.
        val result = AppResult.Success(LinesWithFavourites(listOf(line3), favouriteIds = emptyList()))

        assertEquals(LineDetailUiState.Error(ErrorReason.DATA), LineDetailUiMapper.state(result, lineId = 99))
    }

    @Test
    fun lb103_aFailureIsTheErrorStateWithItsReason() {
        assertEquals(
            LineDetailUiState.Error(ErrorReason.NETWORK),
            LineDetailUiMapper.state(AppResult.Failure(AppError.Network), lineId = 3),
        )
    }

    private fun line(
        id: Int,
        code: String,
        name: String,
    ) = Line(id, code, name, color = 0xFF0FDCFC, onColor = 0xFF000000, LineKind.DAY, isOperational = true)
}
