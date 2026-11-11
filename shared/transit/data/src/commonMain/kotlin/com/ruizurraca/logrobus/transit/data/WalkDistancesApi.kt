package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.network.Decoded
import com.ruizurraca.logrobus.core.network.RequestKind
import com.ruizurraca.logrobus.core.network.SourceClient
import com.ruizurraca.logrobus.core.network.decodeAs
import com.ruizurraca.logrobus.core.network.decodeEach
import com.ruizurraca.logrobus.transit.domain.WalkDestination
import com.ruizurraca.logrobus.transit.domain.WalkDistance
import com.ruizurraca.logrobus.transit.domain.WalkDistances
import com.ruizurraca.logrobus.transit.domain.WalkDistancesRequest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

/** A WGS84 position in a request body (api_spec.md §4.7–4.8). Never logged (NFR-PRIV-02). */
@Serializable
internal data class WalkLatLngDto(
    val lat: Double? = null,
    val lng: Double? = null,
)

/**
 * The body of `POST /api/transit/walk-distances` (api_spec.md §4.8). At most 40 [destinations], pre-filtered by
 * straight-line distance within 800 m, nearest first (FR-HOME-03, FR-STOP-11).
 */
@Serializable
internal data class WalkDistancesRequestDto(
    val origin: WalkLatLngDto? = null,
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
 * [items] stays a [JsonArray] so the reply is decoded item by item and one bad item is dropped and counted instead of
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

/**
 * The writer of the POST body (api_spec.md §4.8), the write-side counterpart of `SourceJson`: a request is only ever
 * written, so none of the reader's flags applies. The two that do make the body exactly the fields the caller filled:
 * - `encodeDefaults = false`, so an empty `destinations` keeps its default and the key stays out of the body;
 * - `explicitNulls = false`, so a property the caller left null is absent rather than `null`. A request must never send
 *   `"lat": null`: the source reads a null as a value, and the request would be refused.
 */
private val RequestJson =
    Json {
        encodeDefaults = false
        explicitNulls = false
    }

/**
 * API-08 (api_spec.md §4.8, FR-HOME-03, FR-STOP-11): the walking distance and duration from the user's position to
 * each stop near it, for Near Me's "Near you" list.
 *
 * It lives in `:shared:transit:data` rather than in a feature's module because two features need it — How to Get There
 * for a route's legs and Near Me for its list — and a shared feature may not depend on another (ModuleRules). The reply
 * is a bare object whose one collection is decoded item by item (api_spec.md §3, NFR-REL-02), so one malformed item
 * does not empty the list.
 */
class WalkDistancesApi(
    private val client: SourceClient,
    private val logger: Logger,
) : WalkDistances {
    /**
     * The walking distance and duration to each destination, whose id comes back as sent (api_spec.md §4.8). An item
     * whose id or distance is missing is dropped and counted rather than failing the list, so one malformed answer
     * never empties the section.
     */
    override suspend fun walkDistances(request: WalkDistancesRequest): AppResult<List<WalkDistance>> =
        when (
            val decoded =
                client
                    .post(
                        PATH_WALK_DISTANCES,
                        RequestJson.encodeToJsonElement(WalkDistancesRequestDto.serializer(), request.toDto()),
                        RequestKind.Live,
                    ).decodeBareCollection(
                        WalkDistancesResponseDto.serializer(),
                        WalkDistancesResponseDto::items,
                        WalkDistanceDto.serializer(),
                    )
        ) {
            is AppResult.Failure -> {
                decoded
            }

            is AppResult.Success -> {
                AppResult.Success(decoded.value.items.mapNotNull { it.toDomain() })
            }
        }

    private fun WalkDistancesRequest.toDto() =
        WalkDistancesRequestDto(
            origin = WalkLatLngDto(lat = originLat, lng = originLng),
            destinations = destinations.map { WalkDestinationDto(id = it.id, lat = it.lat, lng = it.lng) },
        )

    /** The domain value, or `null` for an item the source left without an id or a distance (api_spec.md §3). */
    private fun WalkDistanceDto.toDomain(): WalkDistance? {
        val stopId = id
        val metres = distanceM
        return if (stopId == null || metres == null) {
            null
        } else {
            WalkDistance(stopId = stopId, distanceMeters = metres, durationSeconds = durationSec ?: 0)
        }
    }

    /**
     * The bare-object reply read as [R], then its collection — [collection] of the reply — decoded item by item. A
     * reply whose collection is absent is an empty list, not an error; one that does not decode at all, or whose
     * collection is not an array, fails as `AppError.Data` (api_spec.md §3). A failure passes through untouched.
     */
    private fun <R, T> AppResult<JsonElement>.decodeBareCollection(
        responseSerializer: KSerializer<R>,
        collection: (R) -> JsonArray?,
        itemSerializer: KSerializer<T>,
    ): AppResult<Decoded<T>> =
        when (this) {
            is AppResult.Failure -> {
                this
            }

            is AppResult.Success -> {
                when (val response = value.decodeAs(responseSerializer)) {
                    is AppResult.Failure -> {
                        response
                    }

                    is AppResult.Success -> {
                        val items = collection(response.value) ?: JsonArray(emptyList())
                        AppResult.Success(items.decodeEach(itemSerializer, logger))
                    }
                }
            }
        }

    private companion object {
        const val PATH_WALK_DISTANCES = "/api/transit/walk-distances"
    }
}
