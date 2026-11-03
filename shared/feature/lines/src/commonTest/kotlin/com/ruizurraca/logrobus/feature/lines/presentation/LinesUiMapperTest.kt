package com.ruizurraca.logrobus.feature.lines.presentation

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.feature.lines.domain.LinesWithFavourites
import com.ruizurraca.logrobus.transit.domain.Line
import com.ruizurraca.logrobus.transit.domain.LineKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * LB-101 (FR-LINE-01, FR-LINE-02), LB-103 (FR-LINE-12 to 15) and LB-102 (FR-LINE-03, FR-LINE-16): the Lines list's
 * state, from the lines, the favourite ones and the search, or the failure to get them.
 */
class LinesUiMapperTest {
    @Test
    fun lb101_dayLinesComeFirst_thenNightLines_eachSortedByCode() {
        // The network of lines_2026-10-22.json, shuffled: there is no line 8.
        val codes = listOf("10", "B2", "3", "1", "11", "B1", "9", "4", "7", "B3", "2", "6", "5")

        val state = LinesUiMapper.state(success(codes.map(::line)))

        val day = listOf("1", "2", "3", "4", "5", "6", "7", "9", "10", "11")
        val night = listOf("B1", "B2", "B3")
        val groups = listOf(LineGroupUi(LineKind.DAY, day.map(::row)), LineGroupUi(LineKind.NIGHT, night.map(::row)))
        assertEquals(LinesUiState.Content(favourites = emptyList(), groups = groups), state)
    }

    @Test
    fun lb101_codesSortByTheirNumber_notAsText() {
        val state = LinesUiMapper.state(success(listOf("B10", "B9", "100", "20").map(::line)))

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

        val state = LinesUiMapper.state(success(listOf(stopped)))

        assertEquals(
            LineRowUi(
                id = 3,
                code = "3",
                displayName = "Campillo - El Cubo",
                color = 0xFF0FDCFC,
                onColor = 0xFF000000,
                isOperational = false,
                isFavourite = false,
            ),
            (state as LinesUiState.Content)
                .groups
                .single()
                .lines
                .single(),
        )
    }

    @Test
    fun dd08_linesThatShareAColourKeepTheirOwnCodes() {
        // Lines 4 and B1 share red: each row keeps its code beside the colour, so a badge never stands for a colour.
        val state = LinesUiMapper.state(success(listOf(line("4"), line("B1"))))

        val rows = (state as LinesUiState.Content).groups.flatMap { it.lines }
        assertEquals(listOf("4", "B1"), rows.map { it.code })
        assertEquals(1, rows.map { it.color }.toSet().size)
    }

    @Test
    fun lb101_aKindWithNoLinesHasNoGroup() {
        val state = LinesUiMapper.state(success(listOf(line("B1"))))

        assertEquals(listOf(LineKind.NIGHT), (state as LinesUiState.Content).groups.map { it.kind })
    }

    @Test
    fun lb101_noLinesIsTheEmptyState() {
        assertEquals(LinesUiState.Empty, LinesUiMapper.state(success(emptyList())))
    }

    @Test
    fun lb101_aFailureIsTheErrorStateWithItsReason() {
        assertEquals(
            LinesUiState.Error(ErrorReason.NETWORK),
            LinesUiMapper.state(AppResult.Failure(AppError.Network)),
        )
        assertEquals(LinesUiState.Error(ErrorReason.DATA), LinesUiMapper.state(AppResult.Failure(AppError.Data)))
    }

    @Test
    fun lb103_withNoFavouritesTheCarouselHasNoCards() {
        // FR-LINE-14: each platform shows the hint card in their place; the section never disappears.
        val state = LinesUiMapper.state(success(listOf(line("1"), line("3"))))

        assertEquals(emptyList(), (state as LinesUiState.Content).favourites)
    }

    @Test
    fun lb103_theCarouselHasTheFavouritesInTheUsersOrder_withBadgeNameAndHeadsigns() {
        // FR-LINE-12, FR-LINE-13: the order they were added in, not the list's.
        val line10 =
            line("10").copy(displayName = "El Arco - Hospital San Pedro", headsigns = listOf("H. San Pedro", "El Arco"))
        val state = LinesUiMapper.state(success(listOf(line("3"), line10), favouriteIds = listOf(10, 3)))

        assertEquals(
            listOf(
                FavouriteLineCardUi(
                    id = 10,
                    code = "10",
                    displayName = "El Arco - Hospital San Pedro",
                    color = 0xFFE30613,
                    onColor = 0xFFFFFFFF,
                    headsigns = listOf("H. San Pedro", "El Arco"),
                ),
                FavouriteLineCardUi(3, "3", "Line 3", 0xFFE30613, 0xFFFFFFFF, headsigns = emptyList()),
            ),
            (state as LinesUiState.Content).favourites,
        )
    }

    @Test
    fun dd08_favouriteCardsThatShareAColourKeepTheirOwnCodes() {
        // Lines 4 and B1 share red: each card keeps its code beside the colour, as each row does.
        val state = LinesUiMapper.state(success(listOf(line("4"), line("B1")), favouriteIds = listOf(idOf("B1"), 4)))

        val cards = (state as LinesUiState.Content).favourites
        assertEquals(listOf("B1", "4"), cards.map { it.code })
        assertEquals(1, cards.map { it.color }.toSet().size)
    }

    @Test
    fun lb103_eachRowSaysWhetherItsLineIsAFavourite() {
        // FR-LINE-15: the row's toggle shows it and is labelled by it.
        val state = LinesUiMapper.state(success(listOf(line("1"), line("3"), line("B1")), favouriteIds = listOf(3)))

        val rows = (state as LinesUiState.Content).groups.flatMap { it.lines }
        assertEquals(listOf("3"), rows.filter { it.isFavourite }.map { it.code })
    }

    @Test
    fun lb103_aFavouriteWhoseLineIsNoLongerInTheCatalogueHasNoCard() {
        // It stays saved (FavouriteLinesRepository), and its card comes back if the line does.
        val state = LinesUiMapper.state(success(listOf(line("3")), favouriteIds = listOf(99, 3)))

        assertEquals(listOf("3"), (state as LinesUiState.Content).favourites.map { it.code })
    }

    @Test
    fun lb102_aSearchHidesTheFavourites_andKeepsTheDayAndNightGroups() {
        // FR-LINE-16: the results are the list's own rows, without the carousel.
        val lines = listOf(line("2", "Yagüe - Varea"), line("3", "Campillo - El Cubo"), line("B1", "Varea - La Cava"))

        val state = LinesUiMapper.state(success(lines, favouriteIds = listOf(3)), query = "varea")

        assertEquals(
            LinesUiState.SearchResults(
                listOf(
                    LineGroupUi(LineKind.DAY, listOf(row("2", "Yagüe - Varea"))),
                    LineGroupUi(LineKind.NIGHT, listOf(row("B1", "Varea - La Cava"))),
                ),
            ),
            state,
        )
    }

    @Test
    fun lb102_aRowThatASearchFindsKeepsItsFavouriteStar() {
        val state = LinesUiMapper.state(success(listOf(line("1"), line("3")), favouriteIds = listOf(3)), query = "3")

        assertEquals(
            LinesUiState.SearchResults(listOf(LineGroupUi(LineKind.DAY, listOf(row("3").copy(isFavourite = true))))),
            state,
        )
    }

    @Test
    fun lb102_aClearedSearchBringsTheFavouritesBack() {
        // A query of spaces or punctuation searches nothing, as an empty one.
        val result = success(listOf(line("1"), line("3")), favouriteIds = listOf(3))
        val list = LinesUiMapper.state(result)

        assertIs<LinesUiState.Content>(list)
        assertEquals(list, LinesUiMapper.state(result, query = ""))
        assertEquals(list, LinesUiMapper.state(result, query = " - "))
    }

    @Test
    fun lb102_aSearchThatFindsNothingIsTheNoMatchesState() {
        val state = LinesUiMapper.state(success(listOf(line("1"), line("B1"))), query = "tranvia")

        assertEquals(LinesUiState.NoMatches, state)
    }

    @Test
    fun lb102_withNoLinesOrAFailure_aSearchChangesNothing() {
        assertEquals(LinesUiState.Empty, LinesUiMapper.state(success(emptyList()), query = "varea"))
        assertEquals(
            LinesUiState.Error(ErrorReason.NETWORK),
            LinesUiMapper.state(AppResult.Failure(AppError.Network), query = "varea"),
        )
    }

    private fun line(
        code: String,
        displayName: String,
    ) = line(code).copy(displayName = displayName)

    private fun row(
        code: String,
        displayName: String,
    ) = row(code).copy(displayName = displayName)

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
            isFavourite = false,
        )

    private fun success(
        lines: List<Line>,
        favouriteIds: List<Int> = emptyList(),
    ) = AppResult.Success(LinesWithFavourites(lines, favouriteIds))

    private fun idOf(code: String) = code.filter(Char::isDigit).toInt() + if (code.startsWith("B")) 1_000 else 0
}
