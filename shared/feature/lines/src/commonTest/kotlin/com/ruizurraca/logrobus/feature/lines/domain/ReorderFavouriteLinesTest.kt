package com.ruizurraca.logrobus.feature.lines.domain

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** LB-110 (FR-FAV-02): the edit sheet's new order of the favourite lines. */
class ReorderFavouriteLinesTest {
    private val favourites = FakeFavouriteLines(10, 3, 101)
    private val reorder = DefaultReorderFavouriteLines(favourites)

    @Test
    fun lb110_aNewOrderIsSaved() =
        runTest {
            assertEquals(AppResult.Success(Unit), reorder(listOf(101, 10, 3)))

            assertEquals(AppResult.Success(listOf(101, 10, 3)), favourites.stored.value)
        }

    @Test
    fun lb110_aNewOrderThatCannotBeSavedGivesItsFailure() =
        runTest {
            favourites.failing = true

            assertEquals(AppResult.Failure(AppError.Data), reorder(listOf(101, 10, 3)))
        }
}
