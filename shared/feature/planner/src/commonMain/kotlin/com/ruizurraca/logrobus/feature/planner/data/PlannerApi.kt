package com.ruizurraca.logrobus.feature.planner.data

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.network.Decoded
import com.ruizurraca.logrobus.core.network.RequestKind
import com.ruizurraca.logrobus.core.network.SourceClient
import com.ruizurraca.logrobus.core.network.decodeAs
import com.ruizurraca.logrobus.core.network.decodeEach
import com.ruizurraca.logrobus.feature.planner.data.dto.RouteDto
import com.ruizurraca.logrobus.feature.planner.data.dto.RouteRequestDto
import com.ruizurraca.logrobus.feature.planner.data.dto.RouteResponseDto
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

/**
 * The writer of the POST body (api_spec.md §4.7), the write-side counterpart of `SourceJson`: a request is only
 * ever written, so none of the reader's flags (`ignoreUnknownKeys`, `coerceInputValues`, `isLenient`) applies. The two
 * that do make each body exactly the fields the caller filled:
 * - `encodeDefaults = false`, so a default field keeps its default and the key stays out of the body;
 * - `explicitNulls = false`, so a property the caller left null is absent rather than `null`. A request must never send
 *   `"maxResults": null` or `"lat": null`: the source reads a null as a value, and the request would be refused.
 */
private val RequestJson =
    Json {
        encodeDefaults = false
        explicitNulls = false
    }

/**
 * API-07 as DTOs (architecture.md §3, §4.2): the planner's itinerary service. It is a `POST` with a JSON body and
 * takes the live timeout of spec §8.7, since it runs while a screen waits. API-08, the walking distances, lives in
 * `:shared:transit:data` because Near Me needs it too (LB-203).
 *
 * The reply is not a BFF envelope: it is a bare object whose one collection is decoded item by item (api_spec.md §3,
 * NFR-REL-02). That granularity is the route, so a route holding a leg that does not decode is dropped whole — an
 * itinerary missing a leg is misleading — and the routes around it still reach the screen.
 */
internal class PlannerApi(
    private val client: SourceClient,
    private val logger: Logger,
) {
    /**
     * API-07: at most [RouteRequestDto.maxResults] itineraries between the request's two points, departing at its
     * `departureAt`.
     *
     * A reply with no `routes` key, or with `routes: null`, is a success with no itineraries and never an error: it is
     * how the source answers a request it could not satisfy, carrying the reason in `rejection` (api_spec.md §4.7).
     * [Decoded.dropped] counts the routes dropped whole, so a reply that asked for itineraries and yielded none is told
     * apart from one that was refused.
     */
    suspend fun route(request: RouteRequestDto): AppResult<Decoded<RouteDto>> =
        client
            .post(PATH_ROUTE, RequestJson.encodeToJsonElement(RouteRequestDto.serializer(), request), RequestKind.Live)
            .decodeBareCollection(RouteResponseDto.serializer(), RouteResponseDto::routes, RouteDto.serializer())

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
        const val PATH_ROUTE = "/api/transit/route"
    }
}
