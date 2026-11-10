package com.ruizurraca.logrobus.feature.lines.presentation

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.feature.lines.domain.LinesWithFavourites
import com.ruizurraca.logrobus.transit.domain.Direction
import com.ruizurraca.logrobus.transit.domain.Line
import com.ruizurraca.logrobus.transit.domain.LineKind
import com.ruizurraca.logrobus.transit.domain.LinePattern
import com.ruizurraca.logrobus.transit.domain.LineStop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * LB-104 (FR-LINE-04, DD-16): the line detail's direction toggle and its ordered stop list, from a line's patterns.
 *
 * The map of both shapes is not here: it waits for LB-013 (owner decision, 2026-11-10).
 */
class LineDetailDirectionsTest {
    private val line =
        Line(
            id = 3,
            code = "3",
            displayName = "Campillo - El Cubo",
            color = 0xFFF9A825,
            onColor = 0xFF000000,
            kind = LineKind.DAY,
            isOperational = true,
            headsigns = listOf("El Cubo", "Campillo"),
            stopNames = emptyList(),
            patterns =
                mapOf(
                    Direction.OUTBOUND to
                        LinePattern(
                            headsign = "El Cubo",
                            stops = listOf(stop(1, "Campillo"), stop(2, "Varea"), stop(3, "El Cubo")),
                        ),
                    Direction.INBOUND to
                        LinePattern(
                            headsign = "Campillo",
                            stops = listOf(stop(3, "El Cubo"), stop(2, "Varea"), stop(1, "Campillo")),
                        ),
                ),
        )

    private fun content(
        direction: Direction? = null,
        line: Line = this.line,
    ): LineDetailUiState.Content =
        LineDetailUiMapper.state(
            AppResult.Success(LinesWithFavourites(listOf(line), favouriteIds = emptyList())),
            lineId = line.id,
            direction = direction,
        ) as LineDetailUiState.Content

    @Test
    fun lb104_theToggleOffersBothDirections_eachTowardsItsHeadsign() {
        val state = content()

        assertEquals(
            listOf(
                DirectionOptionUi(Direction.OUTBOUND, "El Cubo", isSelected = true),
                DirectionOptionUi(Direction.INBOUND, "Campillo", isSelected = false),
            ),
            state.directions,
        )
    }

    @Test
    fun lb104_theOutboundIsShownUntilAnotherDirectionIsPicked() {
        // FR-LINE-04: the line opens on one of its directions, deterministically, and the list follows the toggle.
        assertEquals(Direction.OUTBOUND, content().direction)
        assertEquals(Direction.INBOUND, content(Direction.INBOUND).direction)
        assertTrue(content(Direction.INBOUND).directions.single { it.isSelected }.direction == Direction.INBOUND)
    }

    @Test
    fun lb104_theListShowsTheSelectedDirectionsStopsInSequenceOrder() {
        // The criterion: INBOUND shows the INBOUND pattern's stops, in the order they are called at.
        assertEquals(
            listOf("El Cubo", "Varea", "Campillo"),
            content(Direction.INBOUND).stops.map { it.name },
        )
        assertEquals(
            listOf("Campillo", "Varea", "El Cubo"),
            content(Direction.OUTBOUND).stops.map { it.name },
        )
        // The position a screen reader reads: 1 is the first stop called at, in the selected direction.
        assertEquals(listOf(1, 2, 3), content(Direction.INBOUND).stops.map { it.order })
    }

    @Test
    fun lb104_aStopCarriesItsIdAndPosition_forTheMapAndTheStopDetail() {
        val first = content(Direction.OUTBOUND).stops.first()

        assertEquals(StopRowUi(stopId = 1, order = 1, name = "Campillo", lat = 42.46, lng = -2.44), first)
    }

    @Test
    fun lb104_aDirectionTheLineDoesNotRunFallsBackToTheOneItDoes() {
        // A selection that a catalogue change left stale shows a pattern that exists, never an empty list.
        val state =
            content(
                Direction.INBOUND,
                line =
                    line.copy(
                        patterns =
                            mapOf(Direction.OUTBOUND to line.patterns.getValue(Direction.OUTBOUND)),
                    ),
            )

        assertEquals(Direction.OUTBOUND, state.direction)
        assertEquals(1, state.directions.size)
        assertEquals(3, state.stops.size)
    }

    @Test
    fun lb104_aLineWithNoPatternsShowsNoToggleAndNoStops() {
        // It cannot happen from a sync (the engine rejects a line whose pattern stops do not resolve), but the mapper
        // never renders a toggle with nothing to select.
        val state = content(line = line.copy(patterns = emptyMap()))

        assertEquals(emptyList(), state.directions)
        assertEquals(emptyList(), state.stops)
    }

    private fun stop(
        id: Int,
        name: String,
    ) = LineStop(stopId = id, name = name, lat = 42.46, lng = -2.44)
}
