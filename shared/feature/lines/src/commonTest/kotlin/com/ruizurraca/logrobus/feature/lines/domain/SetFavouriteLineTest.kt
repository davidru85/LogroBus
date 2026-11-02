package com.ruizurraca.logrobus.feature.lines.domain

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.telemetry.AnalyticsEvent
import com.ruizurraca.logrobus.core.telemetry.AnalyticsEvent.FavouriteAdd.Kind
import com.ruizurraca.logrobus.core.telemetry.AnalyticsTracker
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** LB-103 (FR-LINE-15, FR-LINE-09): the favourite toggle of a row or of the line detail. */
class SetFavouriteLineTest {
    private val favourites = FakeFavouriteLines(10)
    private val tracked = mutableListOf<AnalyticsEvent>()
    private val analytics =
        object : AnalyticsTracker {
            override fun track(event: AnalyticsEvent) {
                tracked += event
            }
        }
    private val setFavourite = DefaultSetFavouriteLine(favourites, analytics)

    @Test
    fun lb103_addingALineSavesItLast_andCountsAFavouriteAdd() =
        runTest {
            assertEquals(AppResult.Success(Unit), setFavourite(lineId = 3, isFavourite = true))

            assertEquals(AppResult.Success(listOf(10, 3)), favourites.stored.value)
            // NFR-OBS-03: the kind of favourite only, never which line (consent-gated by the tracker).
            assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.FavouriteAdd(Kind.LINE)), tracked)
        }

    @Test
    fun lb103_removingALineRemovesIt_andCountsNothing() =
        runTest {
            assertEquals(AppResult.Success(Unit), setFavourite(lineId = 10, isFavourite = false))

            assertEquals(AppResult.Success(emptyList()), favourites.stored.value)
            assertEquals(emptyList(), tracked)
        }

    @Test
    fun lb103_aSaveThatFailsGivesItsFailure_andCountsNothing() =
        runTest {
            favourites.failing = true

            assertEquals(AppResult.Failure(AppError.Data), setFavourite(lineId = 3, isFavourite = true))
            assertEquals(emptyList(), tracked)
        }
}
