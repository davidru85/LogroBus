package com.ruizurraca.logrobus.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import com.ruizurraca.logrobus.core.common.AppResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals

/** Spec §8.7 and `api_spec.md` §1: the rules every request to the source follows. */
class SourceClientTest {
    /** Every request the client sent, in order. */
    private val requests = mutableListOf<HttpRequestData>()

    private fun client(
        respond: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { json("""{"result":{}}""") },
    ) = SourceClient(
        engine =
            MockEngine { request ->
                requests += request
                respond(request)
            },
        identity = ClientIdentity(appVersion = "1.2.0", platform = "Android", osVersion = "16"),
        baseUrl = { BASE_URL },
    )

    @Test
    fun lb005_everyRequestIdentifiesTheAppAndAcceptsGzippedJson() =
        runTest {
            val client = client()

            client.get("/api/linesDiscovery/lines", RequestKind.Static)
            client.post("/api/transit/walk-distances", buildJsonObject { put("origin", "stop:38") }, RequestKind.Live)

            assertEquals(
                listOf("$BASE_URL/api/linesDiscovery/lines", "$BASE_URL/api/transit/walk-distances"),
                requests.map { it.url.toString() },
            )
            requests.forEach { request ->
                assertEquals("LogronoBus/1.2.0 (Android 16)", request.headers[HttpHeaders.UserAgent])
                assertEquals("application/json", request.headers[HttpHeaders.Accept])
                assertEquals("gzip", request.headers[HttpHeaders.AcceptEncoding])
            }
            val post = requests.last().body as TextContent
            assertEquals(ContentType.Application.Json, post.contentType.withoutParameters())
            assertEquals("""{"origin":"stop:38"}""", post.text)
        }

    @Test
    fun lb005_connectingTimesOutAfterFiveSecondsAndARequestAfterTenOrThirty() =
        runTest {
            val client = client()

            client.get("/api/linesDiscovery/stops", RequestKind.Static)
            client.get("/api/vehicleMonitoring/all", RequestKind.Live)
            client.post("/api/transit/route", buildJsonObject {}, RequestKind.Live)

            val timeouts = requests.map { it.getCapabilityOrNull(HttpTimeoutCapability) }
            assertEquals(listOf(5_000L, 5_000L, 5_000L), timeouts.map { it?.connectTimeoutMillis })
            assertEquals(listOf(30_000L, 10_000L, 10_000L), timeouts.map { it?.requestTimeoutMillis })
        }

    @Test
    fun dd15_anyTwoHundredStatusIsASuccessAndPostsAnswer201() =
        runTest {
            val statuses = listOf(200, 201, 202, 299).map(HttpStatusCode::fromValue)
            // Requests are recorded before they are answered, so the last index is the current request's.
            val client = client { json("""{"ok":true}""", statuses[requests.lastIndex % statuses.size]) }

            val results =
                statuses.map { client.get("/api/vehicleMonitoring/all", RequestKind.Live) } +
                    statuses.map { client.post("/api/transit/route", buildJsonObject {}, RequestKind.Live) }

            assertEquals(List(8) { AppResult.Success(buildJsonObject { put("ok", true) }) }, results)
        }

    private fun MockRequestHandleScope.json(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))

    private companion object {
        const val BASE_URL = "https://transporteurbano.logrono.es"
    }
}
