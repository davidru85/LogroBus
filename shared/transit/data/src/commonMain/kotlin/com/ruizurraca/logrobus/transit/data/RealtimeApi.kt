package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.network.Decoded
import com.ruizurraca.logrobus.core.network.RequestKind
import com.ruizurraca.logrobus.core.network.SourceClient
import com.ruizurraca.logrobus.transit.data.dto.EstimatedCallDto
import com.ruizurraca.logrobus.transit.data.dto.VehicleActivityDto

/**
 * API-03 to API-06 as DTOs (architecture.md §4.2): live vehicle positions and estimated arrivals, for the repositories
 * to map onto the domain. Every call takes the live timeout of spec §8.7.
 *
 * Queries travel as a map, never in the path, because [SourceClient] rejects a path that carries one and because the
 * failure log names the path alone (NFR-PRIV-02).
 */
internal class RealtimeApi(
    private val client: SourceClient,
    private val logger: Logger,
) {
    /** API-03: every monitored vehicle, whether or not the source knows its line (DD-06 leaves that `null`). */
    suspend fun vehicles(): AppResult<Decoded<VehicleActivityDto>> =
        client
            .get("/api/vehicleMonitoring/all", RequestKind.Live)
            .decodeCollection(ACTIVITIES, VehicleActivityDto.serializer(), logger)

    /** API-04: the vehicles of one line. */
    suspend fun vehicles(lineId: Int): AppResult<Decoded<VehicleActivityDto>> =
        client
            .get("/api/vehicleMonitoring/byLine/$lineId", RequestKind.Live)
            .decodeCollection(ACTIVITIES, VehicleActivityDto.serializer(), logger)

    /** API-05: the calls of one line within [previewMinutes] of now. */
    suspend fun arrivals(
        lineId: Int,
        previewMinutes: Int,
    ): AppResult<Decoded<EstimatedCallDto>> =
        client
            .get(
                "/api/estimatedTimetable/byLine/$lineId",
                RequestKind.Live,
                mapOf("previewMinutes" to "$previewMinutes"),
            ).decodeCollection(ARRIVALS, EstimatedCallDto.serializer(), logger)

    /**
     * API-06: the calls at one stop for [lineIds], which the caller takes from the stop's served lines (api_spec.md
     * §4) rather than from the patterns. The list goes out comma-separated, as the source was captured with it.
     */
    suspend fun arrivals(
        stopId: Int,
        lineIds: List<Int>,
        previewMinutes: Int,
    ): AppResult<Decoded<EstimatedCallDto>> =
        client
            .get(
                "/api/estimatedTimetable/byStop/$stopId",
                RequestKind.Live,
                mapOf(
                    "lines" to lineIds.joinToString(","),
                    "previewMinutes" to "$previewMinutes",
                ),
            ).decodeCollection(ARRIVALS, EstimatedCallDto.serializer(), logger)

    private companion object {
        const val ACTIVITIES = "activities"
        const val ARRIVALS = "arrivals"
    }
}
