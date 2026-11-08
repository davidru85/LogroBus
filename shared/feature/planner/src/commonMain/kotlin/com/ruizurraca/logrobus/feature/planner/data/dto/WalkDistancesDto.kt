package com.ruizurraca.logrobus.feature.planner.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

/**
 * The body of `POST /api/transit/walk-distances` (api_spec.md §4.8). At most 40 [destinations], pre-filtered by
 * straight-line distance within 800 m, nearest first (FR-HOME-03, FR-STOP-11).
 */
@Serializable
internal data class WalkDistancesRequestDto(
    val origin: LatLngDto? = null,
    val destinations: List<WalkDestinationDto> = emptyList(),
)

/** One stop the client asks the walking distance to (api_spec.md §4.8): its id, which comes back unchanged. */
@Serializable
internal data class WalkDestinationDto(
    val id: Int? = null,
    val lat: Double? = null,
    val lng: Double? = null,
)

/**
 * The reply to `POST /api/transit/walk-distances` (api_spec.md §4.8), a bare object: `{ "items": [...] }`.
 *
 * [items] stays a [JsonArray] so the mapper decodes it item by item and one bad item is dropped and counted instead of
 * failing the response that feeds the whole nearby list (api_spec.md §3).
 */
@Serializable
internal data class WalkDistancesResponseDto(
    val items: JsonArray? = null,
)

/** The walking distance and duration from the request's origin to one stop (api_spec.md §4.8). */
@Serializable
internal data class WalkDistanceDto(
    val id: Int? = null,
    val distanceM: Int? = null,
    val durationSec: Int? = null,
)
