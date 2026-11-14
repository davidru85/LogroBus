package com.ruizurraca.logrobus.feature.nearme.domain

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.transit.domain.Direction
import com.ruizurraca.logrobus.transit.domain.ServedLine
import com.ruizurraca.logrobus.transit.domain.Stop
import com.ruizurraca.logrobus.transit.domain.StopRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * LB-209 (FR-NEAR-05, FR-STOP-01, FR-STOP-02, DD-16): a stop search that tells its results apart — each with the lines
 * that serve it and the headsign of the journey that reaches it, so two stops with one name are never confused.
 */
class SearchStopsTest {
    private val stops =
        listOf(
            stopWith(38, "Ayuntamiento", lineCode = "1", headsign = "El Arco"),
            stopWith(61, "Ayuntamiento", lineCode = "4", headsign = "Yagüe"),
        )

    @Test
    fun lb209_eachResultCarriesItsLinesAndTheHeadsignThatReachesIt() =
        runTest {
            val results = SearchStopsOf(query = { stops })("Ayuntamiento")

            assertEquals(listOf(38, 61), results.map { it.stop.id })
            val first = results.first()
            assertEquals(listOf("1"), first.lines.map { it.code })
            assertEquals("El Arco", first.lines.single().headsign)
        }

    @Test
    fun lb209_twoStopsWithOneNameAreToldApartByTheirLines() =
        runTest {
            val results = SearchStopsOf(query = { stops })("Ayuntamiento")

            assertEquals(2, results.size, "the two are listed separately, never merged")
            assertTrue(results.map { it.lines.single().code }.toSet() == setOf("1", "4"))
        }

    @Test
    fun lb209_aBlankQueryReturnsNothing() =
        runTest {
            val results = SearchStopsOf(query = { error("a blank query must not reach the database") })("   ")

            assertTrue(results.isEmpty())
        }

    @Test
    fun lb209_aQueryThatFindsNothingReturnsAnEmptyList() =
        runTest {
            val results = SearchStopsOf(query = { emptyList() })("tranvía")

            assertTrue(results.isEmpty())
        }

    private fun stopWith(
        id: Int,
        name: String,
        lineCode: String,
        headsign: String,
    ) = Stop(
        id = id,
        name = name,
        lat = 42.46,
        lng = -2.44,
        isOperational = true,
        isProvisional = false,
        servedLines =
            listOf(
                ServedLine(
                    lineId = lineCode.toInt(),
                    code = lineCode,
                    headsign = headsign,
                    direction = Direction.OUTBOUND,
                    color = 0xFFE30613L,
                    onColor = 0xFFFFFFFFL,
                ),
            ),
    )
}
