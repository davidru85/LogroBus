package com.ruizurraca.logrobus.feature.nearme.domain

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.presentation.EtaDisplay
import com.ruizurraca.logrobus.core.presentation.ServedArrivalUi
import com.ruizurraca.logrobus.favourites.domain.SavedStop
import com.ruizurraca.logrobus.favourites.domain.SavedStopRepository
import com.ruizurraca.logrobus.transit.domain.Stop
import com.ruizurraca.logrobus.transit.domain.StopRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * LB-204 (FR-HOME-02, FR-ERR-03): the calls the section's use case joins onto each saved stop, and the stop whose own
 * read failed. The arrivals arrive already worded, so this checks the join and the failure, not the wording.
 */
class DefaultObserveMyStopsArrivalsTest {
    private fun arrival(code: String) =
        ServedArrivalUi(
            lineCode = code,
            color = 0xFF0FDCFCL,
            onColor = 0xFFFFFFFFL,
            headsign = "El Arco",
            eta = EtaDisplay.Minutes(3),
            isCancelled = false,
            isLowConfidence = false,
        )

    private val saved =
        object : SavedStopRepository {
            override fun observeSavedStops() =
                flowOf(AppResult.Success(listOf(SavedStop(stopId = 38, alias = null, position = 0))))

            override suspend fun add(
                stopId: Int,
                alias: String?,
            ) = AppResult.Success(com.ruizurraca.logrobus.favourites.domain.FavouriteAddition.ADDED)

            override suspend fun rename(
                stopId: Int,
                alias: String?,
            ) = AppResult.Success(Unit)

            override suspend fun remove(stopId: Int) = AppResult.Success(Unit)

            override suspend fun reorder(stopIds: List<Int>) = AppResult.Success(Unit)
        }

    private val stops =
        StopRepository { stopId ->
            flowOf(
                AppResult.Success(
                    Stop(
                        id = stopId,
                        name = "Campillo",
                        lat = 42.46,
                        lng = -2.44,
                        isOperational = true,
                        isProvisional = false,
                        servedLines = emptyList(),
                    ),
                ),
            )
        }

    @Test
    fun lb204_theStopsArrivalsAreJoinedOntoItsRow() =
        runTest {
            val useCase =
                DefaultObserveMyStops(saved, stops) { flowOf(AppResult.Success(listOf(arrival("1"), arrival("3")))) }

            val result = useCase().first()

            val joined = (result as AppResult.Success).value
            assertEquals(listOf("1", "3"), joined.arrivals[38]?.map { it.lineCode })
            assertTrue(joined.failed.isEmpty())
        }

    @Test
    fun lb204_aStopWhoseReadFailedIsMarked_soItsRowKeepsWhatItShowed() =
        runTest {
            val useCase =
                DefaultObserveMyStops(saved, stops) { flowOf(AppResult.Failure(AppError.Network)) }

            val joined = (useCase().first() as AppResult.Success).value

            assertTrue(38 in joined.failed)
            assertTrue(joined.arrivals[38] == null, "no answer means no new list, not an empty one")
        }
}
