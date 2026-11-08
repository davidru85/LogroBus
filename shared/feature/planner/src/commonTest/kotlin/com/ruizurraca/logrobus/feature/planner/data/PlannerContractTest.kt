package com.ruizurraca.logrobus.feature.planner.data

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.LogLevel
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.ServerCorrectedClock
import com.ruizurraca.logrobus.core.network.ClientIdentity
import com.ruizurraca.logrobus.core.network.SourceClient
import com.ruizurraca.logrobus.core.network.decodeAs
import com.ruizurraca.logrobus.core.testing.FixtureLoader
import com.ruizurraca.logrobus.feature.planner.data.dto.LatLngDto
import com.ruizurraca.logrobus.feature.planner.data.dto.LegDto
import com.ruizurraca.logrobus.feature.planner.data.dto.RouteRequestDto
import com.ruizurraca.logrobus.feature.planner.data.dto.RouteResponseDto
import com.ruizurraca.logrobus.feature.planner.data.dto.WalkDestinationDto
import com.ruizurraca.logrobus.feature.planner.data.dto.WalkDistancesRequestDto
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The contract of API-07 and API-08 against the captured payloads (docs/testing_strategy.md §2): both fixtures decode
 * whole, a route with a leg that does not decode costs that route alone (api_spec.md §4.7), and the two POST bodies
 * are the ones the spec documents.
 *
 * The payloads come from `fixtures/` through [FixtureLoader]; the live source is never called (docs/testing_strategy.md
 * §1). The request bodies are read back off the `MockEngine`, which is the only place a request can be seen.
 */
class PlannerContractTest {
    private val requests = mutableListOf<HttpRequestData>()

    private val clock =
        ServerCorrectedClock(
            object : Clock {
                override fun now() = Instant.parse("2026-11-08T08:00:00Z")
            },
        )

    private val logger = RecordingLogger()

    /** A client whose every call answers [body], recording the request. */
    private fun client(body: String) =
        SourceClient(
            engine =
                MockEngine { request ->
                    requests += request
                    // DD-15: both endpoints are POSTs and answer 201, which is still a success.
                    respond(
                        body,
                        HttpStatusCode.Created,
                        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                },
            identity = ClientIdentity(appVersion = "1.0.0", platform = "Android", osVersion = "16"),
            clock = clock,
            baseUrl = { BASE_URL },
            logger = logger,
        )

    /** A client that answers every call with [status] and no body. */
    private fun clientWithStatus(status: HttpStatusCode) =
        SourceClient(
            engine =
                MockEngine {
                    respond(
                        "",
                        status,
                        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                },
            identity = ClientIdentity(appVersion = "1.0.0", platform = "Android", osVersion = "16"),
            clock = clock,
            baseUrl = { BASE_URL },
            logger = logger,
        )

    /** The value of a call that must have succeeded. */
    private fun <T> AppResult<T>.succeeded(): T =
        when (this) {
            is AppResult.Success -> value
            is AppResult.Failure -> fail("Expected a success, got $error")
        }

    /** The error of a call that must have failed. */
    private fun <T> AppResult<T>.failed(): AppError =
        when (this) {
            is AppResult.Success -> fail("Expected a failure, got $value")
            is AppResult.Failure -> error
        }

    /** The JSON text of the one request that went out, parsed so that key order cannot matter. */
    private fun sentBody(): JsonElement {
        val post = requests.single().body as TextContent
        return Json.parseToJsonElement(post.text)
    }

    @Test
    fun lb005_theRouteFixtureDecodesEveryRoute() =
        runTest {
            val fixture = FixtureLoader.read("route_ok.json")
            val api = PlannerApi(client(fixture), logger)

            val decoded = api.route(routeRequest()).succeeded()

            assertEquals(FIXTURE_ROUTES, decoded.items.size)
            assertEquals(0, decoded.dropped)
            assertEquals("$BASE_URL/api/transit/route", requests.single().url.toString())
            assertEquals(HttpMethod.Post, requests.single().method)
            // The order of the legs is the itinerary: walk to the stop, ride, walk to the destination.
            val first = decoded.items.first()
            assertEquals(3, first.legs.size)
            assertIs<LegDto.Walk>(first.legs[0])
            assertIs<LegDto.Bus>(first.legs[1])
            assertIs<LegDto.Walk>(first.legs[2])
            // The reply carries the rules it applied and, because it found routes, no rejection (api_spec.md §4.7).
            val response = Json.parseToJsonElement(fixture).decodeAs(RouteResponseDto.serializer()).succeeded()
            assertEquals(FIXTURE_MAX_WALK_TO_BUS_M, response.criteria?.maxWalkToBusM)
            assertNull(response.rejection, "a reply with routes carries no rejection")
        }

    @Test
    fun lb005_theRejectedRouteFixtureCarriesItsReason() =
        runTest {
            val fixture = FixtureLoader.read("route_rejected.json")
            val api = PlannerApi(client(fixture), logger)

            val decoded = api.route(routeRequest()).succeeded()

            // A refused request is a success with no itineraries: the request was answered, it just cannot be planned.
            assertEquals(0, decoded.items.size)
            assertEquals(0, decoded.dropped)
            val response = Json.parseToJsonElement(fixture).decodeAs(RouteResponseDto.serializer()).succeeded()
            assertEquals("walk-too-far", response.rejection?.reason)
            // `routes` is present and empty, so "no route" is told apart from a reply that lost its key.
            val routes = assertNotNull(response.routes, "the refused reply still carries its routes key")
            assertTrue(routes.isEmpty(), "the refused reply carries no itineraries")
        }

    @Test
    fun lb005_aRouteWithAnUnknownLegModeIsDroppedWhole() =
        runTest {
            // api_spec.md §4.7: a route with a leg that does not decode is misleading, so it goes whole — and only it.
            val api = PlannerApi(client(TWO_ROUTES_ONE_WITH_A_TRAM), logger)

            val decoded = api.route(routeRequest()).succeeded()

            assertEquals(1, decoded.items.size)
            assertEquals(1, decoded.dropped)
            assertIs<LegDto.Walk>(
                decoded.items
                    .single()
                    .legs
                    .single(),
            )
            // The drop is logged by type, index and error type, never with the payload (NFR-PRIV-02).
            assertTrue(
                logger.entries.any {
                    it.level == LogLevel.Warn && it.message.startsWith("Dropped RouteDto at index 1")
                },
                "the dropped route is logged with its index",
            )
        }

    @Test
    fun lb005_theWalkDistancesFixtureDecodesEveryItem() =
        runTest {
            val fixture = FixtureLoader.read("walk_distances.json")
            val api = PlannerApi(client(fixture), logger)

            val decoded = api.walkDistances(walkDistancesRequest()).succeeded()

            assertEquals(2, decoded.items.size)
            assertEquals(0, decoded.dropped)
            assertEquals("$BASE_URL/api/transit/walk-distances", requests.single().url.toString())
            assertEquals(HttpMethod.Post, requests.single().method)
            // The ids come back as sent, which is what pairs a distance with its stop (api_spec.md §4.8).
            assertEquals(listOf(38, 61), decoded.items.mapNotNull { it.id })
            assertEquals(listOf(4906, 395), decoded.items.mapNotNull { it.distanceM })
        }

    @Test
    fun lb005_aReplyThatIsNotJsonFailsAsData() =
        runTest {
            val api = PlannerApi(client("<html>maintenance</html>"), logger)

            assertEquals(AppError.Data, api.route(routeRequest()).failed())
            assertEquals(AppError.Data, api.walkDistances(walkDistancesRequest()).failed())
        }

    @Test
    fun lb005_aReplyWhoseRoutesIsNotAnArrayFailsAsData() =
        runTest {
            val api = PlannerApi(client("""{"criteria":{},"routes":"none"}"""), logger)

            assertEquals(AppError.Data, api.route(routeRequest()).failed())
        }

    @Test
    fun lb005_aReplyWithNoCollectionIsAnEmptySuccess() =
        runTest {
            // A request the source cannot satisfy carries no routes at all; the reason rides in `rejection`.
            val api = PlannerApi(client("""{"criteria":{},"rejection":{"reason":"no-route"}}"""), logger)

            val decoded = api.route(routeRequest()).succeeded()

            assertEquals(0, decoded.items.size)
            assertEquals(0, decoded.dropped)
        }

    @Test
    fun lb005_aFailedCallPassesTheFailureThrough() =
        runTest {
            val api = PlannerApi(clientWithStatus(HttpStatusCode.ServiceUnavailable), logger)

            assertEquals(AppError.Server(503), api.route(routeRequest()).failed())
            assertEquals(AppError.Server(503), api.walkDistances(walkDistancesRequest()).failed())
        }

    @Test
    fun lb005_theRouteRequestSendsTheDocumentedBody() =
        runTest {
            val api = PlannerApi(client(FixtureLoader.read("route_rejected.json")), logger)

            api
                .route(
                    RouteRequestDto(
                        origin = LatLngDto(lat = 42.465, lng = -2.445),
                        destination = LatLngDto(lat = 42.456, lng = -2.43),
                        departureAt = "2026-10-19T07:19:00.000Z",
                        maxResults = 5,
                    ),
                ).succeeded()

            // Parsed trees, not strings: the source reads a JSON object, so its key order is not part of the contract.
            assertEquals(Json.parseToJsonElement(DOCUMENTED_BODY), sentBody())
            // A request never sends a null: the source would read it as a value and refuse the plan.
            assertFalse(sentBody().hasNull(), "the request body must carry no null")
        }

    private fun routeRequest() =
        RouteRequestDto(
            origin = LatLngDto(lat = 42.465, lng = -2.445),
            destination = LatLngDto(lat = 42.456, lng = -2.43),
            departureAt = "2026-10-22T06:19:00.000Z",
            maxResults = 5,
        )

    private fun walkDistancesRequest() =
        WalkDistancesRequestDto(
            origin = LatLngDto(lat = 42.465, lng = -2.445),
            destinations = listOf(WalkDestinationDto(id = 38, lat = 42.426317, lng = -2.461335)),
        )

    private companion object {
        const val BASE_URL = "https://transporteurbano.logrono.es"

        /** Counted with `jq` on the file; each of these is a measurement, never an estimate. */
        const val FIXTURE_ROUTES = 5
        const val FIXTURE_MAX_WALK_TO_BUS_M = 700

        /**
         * The request api_spec.md §4.7 documents, field for field: the two points, the departure as an ISO-8601 UTC
         * instant with milliseconds and `maxResults`. Laid out over two lines because the assertion compares parsed
         * trees, where whitespace between tokens is not part of the JSON.
         */
        val DOCUMENTED_BODY =
            """
            {"origin":{"lat":42.465,"lng":-2.445},"destination":{"lat":42.456,"lng":-2.43},
             "departureAt":"2026-10-19T07:19:00.000Z","maxResults":5}
            """.trimIndent()

        /** Two routes, the second of them with a mode the DTOs do not know. */
        val TWO_ROUTES_ONE_WITH_A_TRAM =
            """
            {"routes":[
              {"totalDurationSec":300,"legs":[{"mode":"walk","distanceM":120,"durationSec":95,"polyline":[]}]},
              {"totalDurationSec":900,"legs":[{"mode":"tram","distanceM":80,"durationSec":240,"polyline":[]}]}
            ],"criteria":{"maxWalkToBusM":700,"maxWalkFromBusM":600,"maxBusWaitMin":30,"directMarginMin":15}}
            """.trimIndent()
    }
}

/** Whether any leaf of this JSON is a null, at any depth. */
private fun JsonElement.hasNull(): Boolean =
    when (this) {
        is JsonNull -> true
        is JsonObject -> values.any { it.hasNull() }
        is JsonArray -> any { it.hasNull() }
        else -> false
    }

/** Keeps every entry, for tests that check what was logged. */
private class RecordingLogger : Logger {
    val entries = mutableListOf<Entry>()

    override fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        entries += Entry(level, tag, message, throwable)
    }

    data class Entry(
        val level: LogLevel,
        val tag: String,
        val message: String,
        val throwable: Throwable?,
    )
}
