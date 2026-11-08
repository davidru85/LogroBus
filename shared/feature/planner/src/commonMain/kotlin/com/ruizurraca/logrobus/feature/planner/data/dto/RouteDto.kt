package com.ruizurraca.logrobus.feature.planner.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

/**
 * The reply to `POST /api/transit/route` (api_spec.md §4.7). A bare object, not a BFF envelope, so it is decoded with
 * `decodeAs`. [routes] stays a [JsonArray] so each route is decoded on its own and a route with a leg that does not
 * decode is dropped whole instead of failing the response (api_spec.md §3, §4.7).
 */
@Serializable
internal data class RouteResponseDto(
    val routes: JsonArray? = null,
    val criteria: CriteriaDto? = null,
    // May coexist with routes: the client shows the routes and only shows the rejection when there are none (DD-17).
    val rejection: RejectionDto? = null,
)

/**
 * The body of `POST /api/transit/route` (api_spec.md §4.7): the two ends of the trip, the departure as an ISO-8601 UTC
 * instant with milliseconds (FR-PLAN-03, `PlanDeparture.departureAt` returns the instant it formats), and how many
 * itineraries to ask for (`maxResults = 5`, LB-306).
 *
 * All four are required, because the caller has them before it asks. The encoder writes no null (`RequestJson` in
 * `PlannerApi.kt`), so a half-built coordinate leaves its field out instead of sending `"lng": null`, which the source
 * would read as a value.
 */
@Serializable
internal data class RouteRequestDto(
    val origin: LatLngDto,
    val destination: LatLngDto,
    // ISO-8601 in UTC with milliseconds, as the spec's example sends it: "2026-10-22T06:19:00.000Z".
    val departureAt: String,
    val maxResults: Int,
)

/** api_spec.md §4.7: an itinerary. [legs] fails the whole route when one leg does not decode. */
@Serializable
internal data class RouteDto(
    val totalDurationSec: Int? = null,
    val totalDistanceM: Int? = null,
    val bounds: BoundsDto? = null,
    val legs: List<LegDto> = emptyList(),
)

/** The rectangle that contains the itinerary (api_spec.md §4.7). */
@Serializable
internal data class BoundsDto(
    val ne: LatLngDto? = null,
    val sw: LatLngDto? = null,
)

/** A decoded coordinate, not an encoded polyline (api_spec.md §4.7). The planner's own, not the transit one. */
@Serializable
internal data class LatLngDto(
    val lat: Double? = null,
    val lng: Double? = null,
)

/** A planner stop reference. The id may be unknown, so it is resolved by name and the nearest point (DD-18). */
@Serializable
internal data class StopRefDto(
    val id: Int? = null,
    val name: String? = null,
)

/**
 * The feasibility rules the source applied to the request (api_spec.md §4.7): `maxWalkToBusM` 700,
 * `maxWalkFromBusM` 600, `maxBusWaitMin` 30 and `directMarginMin` 15.
 */
@Serializable
internal data class CriteriaDto(
    val maxWalkToBusM: Int? = null,
    val maxWalkFromBusM: Int? = null,
    val maxBusWaitMin: Int? = null,
    val directMarginMin: Int? = null,
)

/** Why the source found no route (api_spec.md §4.7). [reason] is mapped to a message key by the mapper. */
@Serializable
internal data class RejectionDto(
    val reason: String? = null,
    val details: Map<String, Double>? = null,
)
