package com.ruizurraca.logrobus.core.network

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.LogLevel
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

/** Spec §8.7 and `api_spec.md` §1: the rules every request to the source follows. */
class SourceClientTest {
    /** Every request the client sent, in order. */
    private val requests = mutableListOf<HttpRequestData>()

    private val clock = stoppedClock()
    private val logger = RecordingLogger()

    private fun client(
        baseUrl: String = BASE_URL,
        respond: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { json("""{"result":{}}""") },
    ) = SourceClient(
        engine =
            MockEngine { request ->
                requests += request
                respond(request)
            },
        identity = ClientIdentity(appVersion = "1.2.0", platform = "Android", osVersion = "16"),
        clock = clock,
        baseUrl = { baseUrl },
        logger = logger,
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
    fun lb005_aGetSendsItsQueryWithListsAsTheSourceWritesThem() =
        runTest {
            // API-06 as captured (spec §4): the lines are a comma-separated list. Anything else is percent-encoded.
            val client = client()

            client.get("/api/estimatedTimetable/byStop/38", RequestKind.Live, mapOf("lines" to "1,33", "previewMinutes" to "60"))
            client.get("/api/linesDiscovery/stops", RequestKind.Static, mapOf("q" to "a b&c=d"))

            assertEquals(
                "$BASE_URL/api/estimatedTimetable/byStop/38?lines=1,33&previewMinutes=60",
                requests.first().url.toString(),
            )
            assertEquals("a b&c=d", requests.last().url.parameters["q"])
        }

    @Test
    fun lb005_aFailedCallIsLoggedWithoutItsQuery() =
        runTest {
            val client = client { json("{}", HttpStatusCode.ServiceUnavailable) }

            client.get("/api/estimatedTimetable/byStop/38", RequestKind.Live, mapOf("lines" to "1,33"))

            assertEquals(listOf("GET /api/estimatedTimetable/byStop/38 failed: Server(status=503)"), logger.entries.map { it[2] })
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
            // The engines' own timers: OkHttp reads and writes for 10 s by default, and NSURLSession has only an idle
            // timeout, which also bounds connecting. Both follow the request timeout.
            assertEquals(listOf(30_000L, 10_000L, 10_000L), timeouts.map { it?.socketTimeoutMillis })
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

    @Test
    fun lb005_anyOtherStatusIsAServerErrorWithThatStatus() =
        runTest {
            val statuses = listOf(304, 404, 500, 503).map(HttpStatusCode::fromValue)
            // A JSON body, as an error page from the BFF could have: the status alone decides.
            val client = client { json("""{"error":"x"}""", statuses[requests.lastIndex % statuses.size]) }

            val results =
                statuses.map { client.get("/api/estimatedTimetable/byLine/1", RequestKind.Live) } +
                    statuses.map { client.post("/api/transit/route", buildJsonObject {}, RequestKind.Live) }

            val expected = statuses.map { AppResult.Failure(AppError.Server(it.value)) }
            assertEquals(expected + expected, results)
        }

    @Test
    fun lb005_aBodyThatIsNotJsonIsADataError() =
        runTest {
            // A proxy's HTML error page, an empty body and a body cut short.
            val bodies = listOf("<html><body>Bad gateway</body></html>", "", """{"result":{"lines":[""")
            val client = client { respond(bodies[requests.lastIndex % bodies.size], HttpStatusCode.OK) }

            val results = bodies.map { client.get("/api/linesDiscovery/lines", RequestKind.Static) }

            assertEquals(List(bodies.size) { AppResult.Failure(AppError.Data) }, results)
        }

    @Test
    fun lb005_aBodyThatIsOnlyAWordOrAScalarIsADataError() =
        runTest {
            // The JSON reader takes an unquoted word as a literal, and every source response is an object or an array.
            val bodies = listOf("<html>Unavailable</html>", "OK", "\"ok\"", "42", "true", "null")
            val client = client { respond(bodies[requests.lastIndex], HttpStatusCode.OK) }

            val results = bodies.map { client.get("/api/linesDiscovery/stops", RequestKind.Static) }

            assertEquals(List(bodies.size) { AppResult.Failure(AppError.Data) }, results)
        }

    @Test
    fun lb005_aTransportFailureOrATimeoutIsANetworkError() =
        runTest {
            val failures =
                listOf(
                    IOException("Unable to resolve host"),
                    ConnectTimeoutException("Connect timeout has expired"),
                    HttpRequestTimeoutException("$BASE_URL/api/vehicleMonitoring/all", 10_000),
                )
            val client = client { throw failures[requests.lastIndex % failures.size] }

            val results =
                failures.map { client.get("/api/vehicleMonitoring/all", RequestKind.Live) } +
                    failures.map { client.post("/api/transit/walk-distances", buildJsonObject {}, RequestKind.Live) }

            assertEquals(List(2 * failures.size) { AppResult.Failure(AppError.Network) }, results)
        }

    @Test
    fun lb005_anyOtherFailureToSendIsANetworkErrorForThatCallOnly() =
        runTest {
            // A redirect loop on a proxy, and an engine that cancels the call while its caller is still active.
            val failures = listOf(IllegalStateException("Max send count exceeded"), CancellationException("Closed"))
            val client = client { throw failures[requests.lastIndex] }

            val results = failures.map { client.get("/api/vehicleMonitoring/all", RequestKind.Live) }

            assertEquals(List(failures.size) { AppResult.Failure(AppError.Network) }, results)
            assertEquals(
                listOf(
                    "GET /api/vehicleMonitoring/all failed: Network (IllegalStateException)",
                    "GET /api/vehicleMonitoring/all failed: Network (CancellationException)",
                ),
                logger.entries.map { it[2] },
            )
        }

    @Test
    fun lb005_aMalformedBaseUrlFailsTheCallNotTheCaller() =
        runTest {
            // api_base_url comes from remote config (spec §8.10).
            val client = client(baseUrl = "https://transporteurbano.logrono.es:443x")

            val result = client.post("/api/transit/route", buildJsonObject {}, RequestKind.Live)

            assertEquals(AppResult.Failure(AppError.Network), result)
            assertEquals(emptyList(), requests)
        }

    @Test
    fun lb005_aCancelledCallIsCancelledNotFailed() =
        runTest {
            // A screen that leaves cancels its call; reporting that as a Network error would show an error state.
            val started = CompletableDeferred<Unit>()
            val client =
                client {
                    started.complete(Unit)
                    awaitCancellation()
                }
            var result: AppResult<JsonElement>? = null

            val call = launch { result = client.get("/api/vehicleMonitoring/all", RequestKind.Live) }
            started.await()
            call.cancelAndJoin()

            assertNull(result)
        }

    @Test
    fun lb005_theDateOfEveryResponseCorrectsTheAppClock() =
        runTest {
            // The server is about 31 s ahead of the device; error responses carry its clock too.
            val responses =
                listOf(
                    HttpStatusCode.OK to "Tue, 27 Oct 2026 08:00:30 GMT",
                    HttpStatusCode.ServiceUnavailable to "Tue, 27 Oct 2026 08:00:32 GMT",
                    HttpStatusCode.Created to "Tue, 27 Oct 2026 08:00:31 GMT",
                )
            val client =
                client {
                    val (status, date) = responses[requests.lastIndex]
                    respond("{}", status, headersOf(HttpHeaders.Date, date))
                }

            responses.forEach { client.get("/api/vehicleMonitoring/all", RequestKind.Live) }

            // The median of 30, 32 and 31 s: all three responses were sampled.
            assertEquals(START + 31.seconds, clock.now())
        }

    @Test
    fun lb005_aCachedResponseIsDatedByItsAge() =
        runTest {
            // A caching proxy behind api_base_url (spec §8.10) keeps the origin's Date and adds Age (RFC 9111 §5.1).
            // Two 40-minute-old static responses would otherwise outvote one live response.
            val responses =
                listOf(
                    cached(date = "Tue, 27 Oct 2026 07:20:30 GMT", age = "2400"),
                    cached(date = "Tue, 27 Oct 2026 07:20:31 GMT", age = "2400"),
                    headersOf(HttpHeaders.Date, "Tue, 27 Oct 2026 08:00:32 GMT"),
                    // An Age that cannot be read says nothing about how old the Date is.
                    cached(date = "Tue, 27 Oct 2026 07:00:00 GMT", age = "-1"),
                    cached(date = "Tue, 27 Oct 2026 07:00:00 GMT", age = "1h"),
                )
            val client = client { respond("{}", HttpStatusCode.OK, responses[requests.lastIndex]) }

            responses.forEach { client.get("/api/linesDiscovery/lines", RequestKind.Static) }

            // The median of 30, 31 and 32 s.
            assertEquals(START + 31.seconds, clock.now())
        }

    @Test
    fun lb005_aDateFromBeforeTheAppExistedLeavesTheAppClockAlone() =
        runTest {
            // A proxy whose clock was never set would move the app clock by decades.
            val dates = listOf("Thu, 01 Jan 1970 00:00:00 GMT", "Wed, 31 Dec 2025 23:59:59 GMT")
            val client =
                client { respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.Date, dates[requests.lastIndex])) }

            dates.forEach { client.get("/api/vehicleMonitoring/all", RequestKind.Live) }

            assertEquals(START, clock.now())
        }

    @Test
    fun lb005_aMissingOrUnreadableDateLeavesTheAppClockAlone() =
        runTest {
            val dates = listOf(null, "yesterday", "Tue, 27 Oct 2026 25:00:30 GMT")
            val client =
                client {
                    val date = dates[requests.lastIndex]
                    val headers = if (date == null) headersOf() else headersOf(HttpHeaders.Date, date)
                    respond("{}", HttpStatusCode.OK, headers)
                }

            val results = dates.map { client.get("/api/vehicleMonitoring/all", RequestKind.Live) }

            assertEquals(List(dates.size) { AppResult.Success(buildJsonObject {}) }, results)
            assertEquals(START, clock.now())
        }

    @Test
    fun lb005_aFailedCallIsLoggedWithoutItsBodyOrCoordinates() =
        runTest {
            // NFR-PRIV-02: the request body, the response body and the error message can all hold coordinates.
            val origin = buildJsonObject { putJsonObject("origin") { put("lat", 42.4650) } }
            val responses: List<suspend MockRequestHandleScope.() -> HttpResponseData> =
                listOf(
                    { json("""{"routes":[]}""", HttpStatusCode.Created) },
                    { json("""{"error":"No stop near 42.4650"}""", HttpStatusCode.BadRequest) },
                    { respond("<p>No stop near 42.4650</p>", HttpStatusCode.OK) },
                    { throw IOException("No route to 42.4650") },
                )
            val client = client { responses[requests.lastIndex]() }

            responses.forEach { client.post("/api/transit/route", origin, RequestKind.Live) }

            assertEquals(
                listOf(
                    "POST /api/transit/route failed: Server(status=400)",
                    "POST /api/transit/route failed: Data",
                    "POST /api/transit/route failed: Network (IOException)",
                ),
                logger.entries.map { it[2] },
            )
            logger.entries.forEach { entry ->
                assertEquals(listOf(LogLevel.Warn, "SourceClient", null), listOf(entry[0], entry[1], entry[3]))
                assertFalse("42.46" in "${entry[2]}", "${entry[2]}")
            }
        }

    private fun cached(
        date: String,
        age: String,
    ) = headersOf(HttpHeaders.Date to listOf(date), HttpHeaders.Age to listOf(age))

    private fun MockRequestHandleScope.json(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
}
