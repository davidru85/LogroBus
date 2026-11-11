package com.ruizurraca.logrobus.feature.nearme.domain

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.favourites.domain.FavouriteAddition
import com.ruizurraca.logrobus.favourites.domain.SavedStop
import com.ruizurraca.logrobus.favourites.domain.SavedStopRepository
import com.ruizurraca.logrobus.transit.domain.Stop
import com.ruizurraca.logrobus.transit.domain.StopRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * LB-208 (FR-HOME-02): "My stops" joins the saved stops with the catalogue's names. An alias wins; without one the
 * name shows; a stop the catalogue no longer holds still has its row.
 */
class DefaultObserveMyStopsTest {
    @Test
    fun lb208_theCatalogueNameShows_whenTheUserGaveNoAlias() =
        runTest {
            val stops = DefaultObserveMyStops(saved(listOf(SavedStop(38, null, 0))), catalogue(mapOf(38 to "Varea")))

            val result = stops().last()

            assertEquals(mapOf(38 to "Varea"), (result as AppResult.Success).value.names)
        }

    @Test
    fun lb208_aStopTheCatalogueLost_hasNoName_soTheRowFallsBack() =
        runTest {
            val stops = DefaultObserveMyStops(saved(listOf(SavedStop(99, null, 0))), catalogue(emptyMap()))

            val result = stops().last()

            assertEquals(emptyMap(), (result as AppResult.Success).value.names)
        }

    @Test
    fun lb208_theSavedStopsComeThroughInTheUsersOrder() =
        runTest {
            val savedStops = listOf(SavedStop(61, "Trabajo", 0), SavedStop(38, "Casa", 1))
            val stops = DefaultObserveMyStops(saved(savedStops), catalogue(mapOf(38 to "Varea", 61 to "Yagüe")))

            val result = stops().last()

            assertEquals(savedStops, (result as AppResult.Success).value.stops)
        }

    @Test
    fun lb208_aFailureReadingTheSavedStops_isAFailure_notAnEmptySection() =
        runTest {
            val failing =
                object : SavedStopRepository {
                    override fun observeSavedStops() = flowOf(AppResult.Failure(AppError.Data))

                    override suspend fun add(
                        stopId: Int,
                        alias: String?,
                    ) = AppResult.Failure(AppError.Data)

                    override suspend fun rename(
                        stopId: Int,
                        alias: String?,
                    ) = AppResult.Failure(AppError.Data)

                    override suspend fun remove(stopId: Int) = AppResult.Failure(AppError.Data)

                    override suspend fun reorder(stopIds: List<Int>) = AppResult.Failure(AppError.Data)
                }
            val stops = DefaultObserveMyStops(failing, catalogue(emptyMap()))

            assertIs<AppResult.Failure>(stops().last())
        }

    private fun saved(stops: List<SavedStop>) =
        object : SavedStopRepository {
            override fun observeSavedStops() = flowOf(AppResult.Success(stops))

            override suspend fun add(
                stopId: Int,
                alias: String?,
            ) = AppResult.Success(FavouriteAddition.ADDED)

            override suspend fun rename(
                stopId: Int,
                alias: String?,
            ) = AppResult.Success(Unit)

            override suspend fun remove(stopId: Int) = AppResult.Success(Unit)

            override suspend fun reorder(stopIds: List<Int>) = AppResult.Success(Unit)
        }

    private fun catalogue(names: Map<Int, String>) =
        StopRepository { stopId ->
            val name = names[stopId]
            if (name == null) {
                flowOf(AppResult.Failure(AppError.Data))
            } else {
                flowOf(
                    AppResult.Success(
                        Stop(
                            id = stopId,
                            name = name,
                            lat = 42.46,
                            lng = -2.44,
                            isOperational = true,
                            isProvisional = false,
                        ),
                    ),
                )
            }
        }
}
