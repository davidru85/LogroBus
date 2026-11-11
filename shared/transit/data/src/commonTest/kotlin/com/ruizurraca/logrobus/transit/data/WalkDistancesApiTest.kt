package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.AppClock
import kotlin.time.Clock
import com.ruizurraca.logrobus.core.network.ClientIdentity
import com.ruizurraca.logrobus.core.network.SourceClient
import com.ruizurraca.logrobus.core.network.decodeAs
import com.ruizurraca.logrobus.core.testing.FixtureLoader
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * LB-203: API-08 as `:shared:transit:data` serves it. Moved out of the planner's module because Near Me needs it too,
 * and a shared feature may not depend on another feature (ModuleRules); the wire contract it pins is api_spec.md §4.8,
 * unchanged by the move.
 */
class WalkDistancesApiTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val logger = QuietLogger()

    @Test
    fun lb203_theWalkDistancesFixtureDecodesEveryItem() =
        runTest {
            val api = WalkDistancesApi(client(FixtureLoader.read("walk_distances.json")), logger)

            val decoded = api.walkDistances(request()).succeeded()

            assertEquals(2, decoded.items.size)
            assertEquals(0, decoded.dropped)
            assertEquals("$BASE_URL/api/transit/walk-distances", requests.single().url.toString())
            assertEquals(HttpMethod.Post, requests.single().method)
            // api_spec.md §4.8: the ids come back as sent, which is what pairs a distance with its stop.
            assertEquals(listOf(38, 61), decoded.items.mapNotNull { it.id })
            assertEquals(listOf(4906, 395), decoded.items.mapNotNull { it.distanceM })
        }

    @Test
    fun lb203_theRequestSendsOnlyTheFieldsItHas() =
        runTest {
            val api = WalkDistancesApi(client("""{"items":[]}"""), logger)

            api.walkDistances(request()).succeeded()

            // api_spec.md §4.8 and §3: a null would be read as a value, so an unfilled field stays out of the body.
            val body = requests.single().jsonBody()
            assertEquals(setOf("origin", "destinations"), body.keys)
            val origin = body.getValue("origin") as JsonObject
            assertEquals(setOf("lat", "lng"), origin.keys)
            val destination = (body.getValue("destinations") as kotlinx.serialization.json.JsonArray).first() as JsonObject
            assertEquals(setOf("id", "lat", "lng"), destination.keys)
        }

    @Test
    fun lb203_aReplyThatIsNotJsonFailsAsData() =
        runTest {
            val api = WalkDistancesApi(client("<html>maintenance</html>"), logger)

            assertEquals(AppError.Data, api.walkDistances(request()).failed())
        }

    @Test
    fun lb203_aFailedCallPassesTheFailureThrough() =
        runTest {
            val api = WalkDistancesApi(clientWithStatus(HttpStatusCode.ServiceUnavailable), logger)

            assertEquals(AppError.Server(503), api.walkDistances(request()).failed())
        }

    /** api_spec.md §4.8: the origin, and one stop the client asks the walking distance to. */
    private fun request() =
        WalkDistancesRequestDto(
            origin = WalkLatLngDto(lat = 42.4650, lng = -2.4450),
            destinations = listOf(WalkDestinationDto(id = 38, lat = 42.426317, lng = -2.461335)),
        )

    private fun client(body: String) =
        SourceClient(
            engine =
                MockEngine { request ->
                    requests += request
                    respond(
                        body,
                        HttpStatusCode.Created,
                        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                },
            identity = ClientIdentity(appVersion = "1.0.0", platform = "Android", osVersion = "16"),
            clock = AppClock { Clock.System.now() },
            baseUrl = { BASE_URL },
            logger = logger,
        )

    private fun clientWithStatus(status: HttpStatusCode) =
        SourceClient(
            engine = MockEngine { respond("", status) },
            identity = ClientIdentity(appVersion = "1.0.0", platform = "Android", osVersion = "16"),
            clock = AppClock { Clock.System.now() },
            baseUrl = { BASE_URL },
            logger = logger,
        )

    private fun HttpRequestData.jsonBody(): JsonObject =
        Json.parseToJsonElement((body as io.ktor.http.content.TextContent).text).decodeAs(JsonObject.serializer())

    private fun <T> AppResult<T>.succeeded(): T =
        when (this) {
            is AppResult.Success -> value
            is AppResult.Failure -> error("Expected a success, got $error")
        }

    private fun <T> AppResult<T>.failed(): AppError =
        when (this) {
            is AppResult.Success -> error("Expected a failure, got $value")
            is AppResult.Failure -> error
        }

    private class QuietLogger : Logger {
        override fun log(
            level: com.ruizurraca.logrobus.core.common.LogLevel,
            tag: String,
            message: String,
            throwable: Throwable?,
        ) = Unit
    }

    private companion object {
        const val BASE_URL = "https://example.test"
    }
}
