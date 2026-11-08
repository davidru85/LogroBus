package com.ruizurraca.logrobus.feature.planner.data.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * One leg of an itinerary (api_spec.md §4.7), discriminated by `mode`. The source sends `"walk"` and `"bus"`
 * only; both carry `distanceM`, `durationSec` and a decoded `polyline`.
 *
 * The compiler plugin generates a sealed serializer that already knows both subclasses, so `SourceJson` needs no
 * `serializersModule` to decode this: the discriminator is read from the descriptor's `@JsonClassDiscriminator`.
 */
@OptIn(ExperimentalSerializationApi::class) // JsonClassDiscriminator
@Serializable
@JsonClassDiscriminator("mode")
internal sealed interface LegDto {
    @Serializable
    @SerialName("walk")
    data class Walk(
        val distanceM: Int? = null,
        val durationSec: Int? = null,
        val polyline: List<LatLngDto> = emptyList(),
    ) : LegDto

    /** api_spec.md §4.7 and DD-18: `fromStop` and `toStop` ids may be outside the API-02 range. */
    @Serializable
    @SerialName("bus")
    data class Bus(
        val distanceM: Int? = null,
        val durationSec: Int? = null,
        val polyline: List<LatLngDto> = emptyList(),
        val lineId: Int? = null,
        val lineName: String? = null,
        // Hex, e.g. "#C100FB"; the API-01 colour by lineId wins when it can be parsed (DD-08).
        val lineColor: String? = null,
        val direction: String? = null, // "asc" | "desc", per DD-04
        val fromStop: StopRefDto? = null,
        val toStop: StopRefDto? = null,
        val intermediateStops: List<StopRefDto> = emptyList(), // excludes fromStop and toStop
        // Planned (timetable) times, epoch milliseconds: the real-time ETA is overlaid from API-06 (FR-PLAN-06).
        val departureEpochMs: Long? = null,
        val arrivalEpochMs: Long? = null,
    ) : LegDto
}
