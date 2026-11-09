package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.ServerCorrectedClock
import com.ruizurraca.logrobus.core.network.ClientIdentity
import com.ruizurraca.logrobus.core.network.SourceClient
import com.ruizurraca.logrobus.core.testing.FixtureLoader
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Spec §8.5 and LB-008 part 2: the source hands the sync the raw payloads of API-01 and API-02, so the hash covers
 * what the source sent and the normaliser decodes it again through the DTOs (architecture.md §4.4).
 *
 * The payloads come from `fixtures/` through [FixtureLoader]; the live source is never called
 * (docs/testing_strategy.md §1).
 */
class SourceStaticSourceTest {
    /** Every request the engine saw, in order. */
    private val requests = mutableListOf<HttpRequestData>()

    private val clock =
        ServerCorrectedClock(
            object : Clock {
                override fun now() = Instant.parse("2026-11-09T08:00:00Z")
            },
        )

    private val logger: Logger = RecordingLogger()

    /** A client whose every call answers [body] with [status], recording the request. */
    private fun client(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = SourceClient(
        engine =
            MockEngine { request ->
                requests += request
                respond(
                    body,
                    status,
                    headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        identity = ClientIdentity(appVersion = "1.0.0", platform = "Android", osVersion = "16"),
        clock = clock,
        baseUrl = { BASE_URL },
        logger = logger,
    )

    /** How many items the envelope of api_spec.md §3 carries under [key]. */
    private fun JsonElement.items(key: String): Int =
        jsonObject
            .getValue("result")
            .jsonObject
            .getValue(key)
            .jsonArray.size

    private fun <T> AppResult<T>.succeeded(): T =
        when (this) {
            is AppResult.Success -> value
            is AppResult.Failure -> fail("Expected a success, got $error")
        }

    private fun <T> AppResult<T>.failed(): AppError =
        when (this) {
            is AppResult.Success -> fail("Expected a failure, got $value")
            is AppResult.Failure -> error
        }

    @Test
    fun lb008_theSourceHandsTheSyncTheRawLinePayload() =
        runTest {
            val source = SourceStaticSource(client(FixtureLoader.read("lines_2026-10-22.json")))

            val element = source.lines().succeeded()

            // The whole body, untouched: the sync hashes it and the normaliser decodes it again.
            assertEquals(FIXTURE_LINES, element.items("lines"))
            assertEquals("$BASE_URL/api/linesDiscovery/lines", requests.single().url.toString())
            // Spec §8.7: the static endpoints take the static timeout, since they run in the daily sync.
            val timeout = requests.single().getCapabilityOrNull(HttpTimeoutCapability)
            assertEquals(STATIC_TIMEOUT, timeout?.requestTimeoutMillis)
            assertEquals(STATIC_TIMEOUT, timeout?.socketTimeoutMillis)
        }

    @Test
    fun lb008_theSourceHandsTheSyncTheRawStopPayload() =
        runTest {
            val source = SourceStaticSource(client(FixtureLoader.read("stops_2026-10-22.json")))

            val element = source.stops().succeeded()

            assertEquals(FIXTURE_STOPS, element.items("stops"))
            assertEquals("$BASE_URL/api/linesDiscovery/stops", requests.single().url.toString())
            val timeout = requests.single().getCapabilityOrNull(HttpTimeoutCapability)
            assertEquals(STATIC_TIMEOUT, timeout?.requestTimeoutMillis)
            assertEquals(STATIC_TIMEOUT, timeout?.socketTimeoutMillis)
        }

    @Test
    fun lb008_aFailedCallPassesTheFailureThrough() =
        runTest {
            // NFR-REL-02: the status decides, and a failure costs its own call. No retry, no second request.
            val source = SourceStaticSource(client("""{"result":{}}""", HttpStatusCode.ServiceUnavailable))

            assertEquals(AppError.Server(503), source.lines().failed())
            assertEquals(1, requests.size, "lines: one request, never a retry")

            assertEquals(AppError.Server(503), source.stops().failed())
            assertEquals(2, requests.size, "stops: one request, never a retry")
        }

    private companion object {
        const val BASE_URL = "https://transporteurbano.logrono.es"

        /** Spec §8.7. */
        const val STATIC_TIMEOUT = 30_000L

        /** Counted with `jq` on the file; each of these is a measurement, never an estimate. */
        const val FIXTURE_LINES = 13
        const val FIXTURE_STOPS = 254
    }
}
