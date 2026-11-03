package com.ruizurraca.logrobus.feature.lines.domain

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.telemetry.AnalyticsEvent
import com.ruizurraca.logrobus.core.telemetry.AnalyticsEvent.FavouriteAdd.Kind
import com.ruizurraca.logrobus.core.telemetry.AnalyticsTracker
import com.ruizurraca.logrobus.favourites.domain.FAVOURITES_PER_TYPE
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * LB-103 (FR-LINE-15, FR-LINE-09): the favourite toggle of a row or of the line detail. LB-110 (FR-FAV-05): it saves at
 * most 20 lines.
 */
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
            assertEquals(AppResult.Success(FavouriteToggle.Done), setFavourite(lineId = 3, isFavourite = true))

            assertEquals(AppResult.Success(listOf(10, 3)), favourites.stored.value)
            // NFR-OBS-03: the kind of favourite only, never which line (consent-gated by the tracker).
            assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.FavouriteAdd(Kind.LINE)), tracked)
        }

    @Test
    fun lb103_addingALineThatIsAlreadyAFavouriteCountsNothing() =
        runTest {
            // A double tap sends "add" twice before the toggle shows the first; only one favourite was added.
            assertEquals(AppResult.Success(FavouriteToggle.Done), setFavourite(lineId = 10, isFavourite = true))

            assertEquals(AppResult.Success(listOf(10)), favourites.stored.value)
            assertEquals(emptyList(), tracked)
        }

    @Test
    fun lb103_removingALineRemovesIt_andCountsNothing() =
        runTest {
            assertEquals(AppResult.Success(FavouriteToggle.Done), setFavourite(lineId = 10, isFavourite = false))

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

    @Test
    fun lb110_aLineBeyondTheLimitIsNotSaved_andTheToggleSaysWhy_andCountsNothing() =
        runTest {
            val full = FakeFavouriteLines(*IntArray(FAVOURITES_PER_TYPE) { it + 1 })
            val setFavourite = DefaultSetFavouriteLine(full, analytics)

            assertEquals(
                AppResult.Success(FavouriteToggle.LimitReached(limit = FAVOURITES_PER_TYPE)),
                setFavourite(lineId = 101, isFavourite = true),
            )
            assertEquals(AppResult.Success((1..FAVOURITES_PER_TYPE).toList()), full.stored.value)
            assertEquals(emptyList(), tracked)
        }

    @Test
    fun lb110_aLineThatIsAlreadyAFavouriteIsDone_evenAtTheLimit() =
        runTest {
            // A double tap at 20: the line is saved, so the toggle shows it without a limit message.
            val full = FakeFavouriteLines(*IntArray(FAVOURITES_PER_TYPE) { it + 1 })

            assertEquals(
                AppResult.Success(FavouriteToggle.Done),
                DefaultSetFavouriteLine(full, analytics)(lineId = 5, isFavourite = true),
            )
        }
}
