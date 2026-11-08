package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.ServerCorrectedClock
import com.ruizurraca.logrobus.core.network.ClientIdentity
import com.ruizurraca.logrobus.core.network.SourceClient
import com.ruizurraca.logrobus.core.testing.FixtureLoader
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The contract of the DTOs against the captured payloads (docs/testing_strategy.md §2): every fixture decodes whole,
 * with the counts and the fields the spec pins (api_spec.md §4), and one malformed item costs only itself.
 *
 * The payloads come from `fixtures/` through [FixtureLoader]; the live source is never called
 * (docs/testing_strategy.md §1).
 */
class TransitContractTest {
    private val requests = mutableListOf<HttpRequestData>()

    private val clock =
        ServerCorrectedClock(
            object : Clock {
                override fun now() = Instant.parse("2026-11-08T08:00:00Z")
            },
        )

    private val logger: Logger = RecordingLogger()

    /** A client whose every call answers [body], recording the request. */
    private fun client(body: String) =
        SourceClient(
            engine =
                MockEngine { request ->
                    requests += request
                    respond(
                        body,
                        HttpStatusCode.OK,
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

    @Test
    fun lb005_theLineFixtureDecodesEveryLine() =
        runTest {
            val api = LinesApi(client(FixtureLoader.read("lines_2026-10-22.json")), logger)

            val decoded = api.lines().succeeded()

            assertEquals(FIXTURE_LINES, decoded.items.size)
            assertEquals(0, decoded.dropped)
            assertEquals("$BASE_URL/api/linesDiscovery/lines", requests.single().url.toString())
            // Line 8 is not served; the ids are the source's, not positions in the list.
            val ids = decoded.items.mapNotNull { it.id }
            assertFalse(8 in ids, "line 8 is not served, so it must not appear")
            assertTrue(
                decoded.items.any { it.name == "B3-LARDERO-EL CAMPILLO" },
                "the B3 line is served and must decode",
            )
            decoded.items.forEach { line ->
                // Every line draws both ways (api_spec.md §5); a pattern-less direction would break the map.
                val patterns = assertNotNull(line.stops, "line ${line.id} carries no patterns")
                assertTrue(patterns.asc.isNotEmpty(), "line ${line.id} has no ascending pattern")
                assertTrue(patterns.desc.isNotEmpty(), "line ${line.id} has no descending pattern")
            }
        }

    @Test
    fun lb005_theStopFixtureDecodesEveryStop() =
        runTest {
            val api = LinesApi(client(FixtureLoader.read("stops_2026-10-22.json")), logger)

            val decoded = api.stops().succeeded()

            assertEquals(FIXTURE_STOPS, decoded.items.size)
            assertEquals(0, decoded.dropped)
            assertEquals("$BASE_URL/api/linesDiscovery/stops", requests.single().url.toString())
            val stop38 = decoded.items.single { it.id == 38 }
            // API-02's `lines` is what API-06 asks with, so it must survive decoding (api_spec.md §4).
            assertTrue(1 in stop38.lines, "stop 38 is served by line 1, among others")
        }

    @Test
    fun lb005_theVehicleFixtureKeepsEveryActivityIncludingTheObjectObjectOnes() =
        runTest {
            val api = RealtimeApi(client(FixtureLoader.read("vm_all_with_object_lineRef.json")), logger)

            val decoded = api.vehicles().succeeded()

            assertEquals(FIXTURE_ACTIVITIES, decoded.items.size)
            assertEquals(0, decoded.dropped)
            assertEquals("$BASE_URL/api/vehicleMonitoring/all", requests.single().url.toString())
            // DD-06: the source sends "[object Object]" for most of the fleet, and the DTO keeps the string it is.
            assertEquals(
                FIXTURE_OBJECT_OBJECT_ACTIVITIES,
                decoded.items.count { it.lineRef == "[object Object]" },
            )
        }

    @Test
    fun lb005_theLineVehicleFixtureDecodes() =
        runTest {
            val api = RealtimeApi(client(FixtureLoader.read("vm_line2.json")), logger)

            val decoded = api.vehicles(lineId = 2).succeeded()

            assertEquals(FIXTURE_LINE_2_ACTIVITIES, decoded.items.size)
            assertEquals(0, decoded.dropped)
            assertEquals("$BASE_URL/api/vehicleMonitoring/byLine/2", requests.single().url.toString())
            assertTrue(decoded.items.all { it.lineRef == "2" }, "API-04 answers only the requested line")
        }

    @Test
    fun lb005_theLineArrivalsFixtureCarriesBothDirectionCodes() =
        runTest {
            val api = RealtimeApi(client(FixtureLoader.read("et_line1_60min.json")), logger)

            val decoded = api.arrivals(lineId = 1, previewMinutes = 60).succeeded()

            assertEquals(FIXTURE_LINE_1_ARRIVALS, decoded.items.size)
            assertEquals(0, decoded.dropped)
            // API-05's preview goes out as a query parameter, never in the path (api_spec.md §1).
            assertEquals(
                "$BASE_URL/api/estimatedTimetable/byLine/1?previewMinutes=60",
                requests.single().url.toString(),
            )
            // DD-04: the codes are opaque; both directions of the line appear in the capture.
            val directions = decoded.items.mapNotNull { it.directionRef }.toSet()
            assertTrue("401" in directions, "the capture holds calls of direction 401")
            assertTrue("402" in directions, "the capture holds calls of direction 402")
        }

    @Test
    fun lb005_theStopArrivalsFixtureDecodes() =
        runTest {
            val api = RealtimeApi(client(FixtureLoader.read("et_stop38.json")), logger)

            val decoded = api.arrivals(stopId = 38, lineIds = listOf(1, 33), previewMinutes = 60).succeeded()

            assertEquals(FIXTURE_STOP_38_ARRIVALS, decoded.items.size)
            assertEquals(0, decoded.dropped)
            // API-06: the lines go out comma-separated, as the source was captured with them (api_spec.md §1).
            assertEquals(
                "$BASE_URL/api/estimatedTimetable/byStop/38?lines=1,33&previewMinutes=60",
                requests.single().url.toString(),
            )
            assertTrue(decoded.items.all { it.directionRef == "401" }, "every captured call is of direction 401")
        }

    @Test
    fun lb005_oneMalformedActivityIsDroppedAndTheRestKept() =
        runTest {
            // NFR-REL-02: a wrong-typed field costs its own item, never the response.
            val api =
                RealtimeApi(
                    client(
                        """
                        {"result":{"activities":[
                          {"vehicleRef":"2847","lineRef":"2","latitude":42.463141,"longitude":-2.463196},
                          {"vehicleRef":"2883","lineRef":"2","latitude":"north","longitude":-2.42653},
                          {"vehicleRef":"9693","lineRef":"[object Object]","latitude":42.44987,"longitude":-2.412502}
                        ]}}
                        """.trimIndent(),
                    ),
                    logger,
                )

            val decoded = api.vehicles().succeeded()

            assertEquals(listOf("2847", "9693"), decoded.items.map { it.vehicleRef })
            assertEquals(1, decoded.dropped)
        }

    private companion object {
        const val BASE_URL = "https://transporteurbano.logrono.es"

        /** Counted with `jq` on the file; each of these is a measurement, never an estimate. */
        const val FIXTURE_LINES = 13
        const val FIXTURE_STOPS = 254
        const val FIXTURE_ACTIVITIES = 57
        const val FIXTURE_OBJECT_OBJECT_ACTIVITIES = 40
        const val FIXTURE_LINE_2_ACTIVITIES = 2
        const val FIXTURE_LINE_1_ARRIVALS = 32
        const val FIXTURE_STOP_38_ARRIVALS = 2
    }
}
