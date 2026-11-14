package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.feature.nearme.domain.SearchStops
import com.ruizurraca.logrobus.feature.nearme.domain.StopSearchResultUi
import com.ruizurraca.logrobus.transit.domain.Direction
import com.ruizurraca.logrobus.transit.domain.ServedLine
import com.ruizurraca.logrobus.transit.domain.Stop

/**
 * LB-209: the stop search of a preview or a Swift test, over the sample lines. Its own class, as the saved stops'
 * script is, so `PreviewUseCases` keeps within detekt's function limit.
 *
 * It matches a name by containing the query, folded for case, which is enough to script "the two stops called
 * Ayuntamiento" and the empty answer; the accent folding and the FTS ranking are the database's, tested there.
 */
internal class SampleStopSearch : StopSearchUseCases {
    override fun searchStops(): SearchStops = SearchStops { query -> search(query) }

    private fun search(query: String): List<StopSearchResultUi> {
        if (query.isBlank()) return emptyList()
        return SampleLines.all
            .flatMap { line -> line.patterns.values.flatMap { pattern -> pattern.stops } }
            .distinctBy { it.stopId }
            // FR-STOP-01: a number finds that stop, as the database's FTS ranks it first; anything else matches a name.
            .filter { sample ->
                sample.stopId == query.trim().toIntOrNull() ||
                    sample.name.contains(query.trim(), ignoreCase = true)
            }.map { sample ->
                StopSearchResultUi(
                    Stop(
                        id = sample.stopId,
                        name = sample.name,
                        lat = sample.lat,
                        lng = sample.lng,
                        isOperational = true,
                        isProvisional = false,
                        servedLines =
                            SampleLines.all
                                .filter { line ->
                                    line.patterns.values.any { pattern ->
                                        pattern.stops.any { it.stopId == sample.stopId }
                                    }
                                }.map { line ->
                                    ServedLine(
                                        lineId = line.id,
                                        code = line.code,
                                        headsign =
                                            line.patterns.values
                                                .first()
                                                .headsign,
                                        direction = Direction.OUTBOUND,
                                        color = line.color,
                                        onColor = line.onColor,
                                    )
                                },
                    ),
                )
            }
    }
}
