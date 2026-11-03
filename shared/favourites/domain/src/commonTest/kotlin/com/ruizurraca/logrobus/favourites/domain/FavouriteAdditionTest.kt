package com.ruizurraca.logrobus.favourites.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/** LB-110 (FR-FAV-05, requirements.md §3.6): what saving a favourite can do, and the limit of each type. */
class FavouriteAdditionTest {
    @Test
    fun lb110_theLimitIsTwentyOfEachType() {
        assertEquals(20, FAVOURITES_PER_TYPE)
    }

    @Test
    fun lb110_savingAFavouriteAddsIt_findsItSaved_orReachesTheLimit() {
        assertEquals(
            listOf(FavouriteAddition.ADDED, FavouriteAddition.ALREADY_SAVED, FavouriteAddition.LIMIT_REACHED),
            FavouriteAddition.entries,
        )
    }
}
