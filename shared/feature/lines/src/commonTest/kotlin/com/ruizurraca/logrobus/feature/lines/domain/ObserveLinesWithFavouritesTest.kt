package com.ruizurraca.logrobus.feature.lines.domain

import app.cash.turbine.test
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.transit.domain.Line
import com.ruizurraca.logrobus.transit.domain.LineKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** LB-103 (FR-LINE-12, FR-LINE-15): the lines, with the favourite ones in the user's order. */
class ObserveLinesWithFavouritesTest {
    private val line3 = line(3)
    private val line10 = line(10)
    private val lines = MutableStateFlow<AppResult<List<Line>>>(AppResult.Success(listOf(line3, line10)))
    private val favourites = FakeFavouriteLines(10)
    private val observe = DefaultObserveLinesWithFavourites({ lines }, favourites)

    @Test
    fun lb103_eachResultHasTheLinesAndTheFavouritesInTheUsersOrder() =
        runTest {
            observe().test {
                assertEquals(AppResult.Success(LinesWithFavourites(listOf(line3, line10), listOf(10))), awaitItem())
            }
        }

    @Test
    fun lb103_aFavouriteAddedReachesTheLinesAtOnce() =
        runTest {
            observe().test {
                awaitItem()
                favourites.add(3)
                assertEquals(listOf(10, 3), (awaitItem() as AppResult.Success<LinesWithFavourites>).value.favouriteIds)
            }
        }

    @Test
    fun lb103_aFailureToGetTheLinesIsTheResult() =
        runTest {
            lines.value = AppResult.Failure(AppError.Network)

            observe().test {
                assertEquals(AppResult.Failure(AppError.Network), awaitItem())
            }
        }

    @Test
    fun lb103_favouritesThatCannotBeReadLeaveTheLinesWithNone() =
        runTest {
            // The lines still show (spec §7.3); the repository has logged the failure.
            favourites.stored.value = AppResult.Failure(AppError.Data)

            observe().test {
                assertEquals(AppResult.Success(LinesWithFavourites(listOf(line3, line10), emptyList())), awaitItem())
            }
        }

    private fun line(id: Int) =
        Line(id, "$id", "Line $id", color = 0xFF0000FF, onColor = 0xFFFFFFFF, LineKind.DAY, isOperational = true)
}
