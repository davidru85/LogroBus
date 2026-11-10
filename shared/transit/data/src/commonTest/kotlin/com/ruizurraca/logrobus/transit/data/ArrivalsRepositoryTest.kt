package com.ruizurraca.logrobus.transit.data

import app.cash.turbine.test
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.LogLevel
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.network.ClientIdentity
import com.ruizurraca.logrobus.core.network.SourceClient
import com.ruizurraca.logrobus.core.polling.PollKey
import com.ruizurraca.logrobus.core.polling.PollingEngine
import com.ruizurraca.logrobus.core.polling.Timed
import com.ruizurraca.logrobus.core.testing.FixtureLoader
import com.ruizurraca.logrobus.transit.domain.Arrival
import com.ruizurraca.logrobus.transit.domain.Direction
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * LB-105 (FR-LINE-05, spec §5.7, DD-04, DD-09): the calls of one line, polled through the one engine and read from the
 * capture of 2026-11-08. Every request is answered by a `MockEngine` from the fixture, never by the source
 * (AGENTS.md §9).
 */
class ArrivalsRepositoryTest {
    private val logged = mutableListOf<String>()

    private val logger =
        object : Logger {
            override fun log(
                level: LogLevel,
                tag: String,
                message: String,
                throwable: Throwable?,
            ) {
                logged += message
            }
        }

    /** The URLs the mock engine saw, and the poll keys the fake engine was asked for. */
    private val recordedUrls = mutableListOf<String>()
    private val recordedKeys = mutableListOf<String>()

    /** A polling engine that fetches once, so the repository's mapping is what these tests exercise. */
    private fun fakePolling() =
        object : PollingEngine {
            override fun <T> poll(
                key: PollKey,
                interval: Duration,
                fetch: suspend () -> AppResult<T>,
            ): Flow<Timed<T>> =
                flow {
                    recordedKeys += key.value
                    when (val result = fetch()) {
                        is AppResult.Success -> emit(Timed(value = result.value, fetchedAt = START, lastError = null))
                        is AppResult.Failure -> emit(Timed(value = null, fetchedAt = START, lastError = result.error))
                    }
                }
        }

    private fun repository(
        body: String = FixtureLoader.read("et_line1_60min.json"),
        patterns: Map<Direction, List<Int>> = emptyMap(),
    ) = ArrivalsRepositoryImpl(
        api = RealtimeApi(client = client(body), logger = logger),
        polling = fakePolling(),
        interval = INTERVAL,
        previewMinutes = PREVIEW_MINUTES,
        logger = logger,
        patternsOf = { patterns },
    )

    private fun client(body: String) =
        SourceClient(
            engine =
                MockEngine { request ->
                    recordedUrls += request.url.toString()
                    respond(
                        body,
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                },
            identity = ClientIdentity("1.0.0", "Android", "16"),
            clock = stoppedClock(),
            baseUrl = { BASE_URL },
            logger = logger,
        )

    @Test
    fun lb105_theLineFixtureYieldsItsCallsSortedByExpectedTime() =
        runTest {
            // jq: 32 calls, 21 with direction 401 and 11 with 402, all on line 1.
            repository().observeLineArrivals(lineId = 1, direction = null).test {
                val first = awaitItem()
                assertIs<AppResult.Success<List<Arrival>>>(first)
                assertEquals(32, first.value.size)
                assertEquals(
                    first.value.map { it.expectedAt }.sorted(),
                    first.value.map { it.expectedAt },
                    "sorted by the instant the source expects the bus",
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun lb105_theRequestAsksForTheConfiguredPreviewWindow() =
        runTest {
            repository().observeLineArrivals(lineId = 1, direction = null).test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(1, recordedKeys.size)
            assertEquals(1, recordedUrls.size)
            assertTrue(
                recordedUrls.single().contains("/api/estimatedTimetable/byLine/1") &&
                    recordedUrls.single().contains("previewMinutes=$PREVIEW_MINUTES"),
                recordedUrls.single(),
            )
        }

    @Test
    fun lb105_aNumericDirectionCodeStaysUnknown_soNoCallIsAttributedToAWrongPattern() =
        runTest {
            // DD-04 and spec §5.6: ET's directionRef is an opaque code (401, 402), and decoding it into a pattern is
            // the inferrer's job, which needs the line's stops. This repository does not guess: it keeps the call
            // with a null direction, so asking for a direction leaves it out rather than putting a bus on the wrong
            // pattern.
            repository().observeLineArrivals(lineId = 1, direction = null).test {
                val first = awaitItem()
                assertIs<AppResult.Success<List<Arrival>>>(first)
                assertTrue(first.value.all { it.direction == null }, "the capture's codes are numeric, not words")
                cancelAndIgnoreRemainingEvents()
            }

            repository().observeLineArrivals(lineId = 1, direction = Direction.OUTBOUND).test {
                val first = awaitItem()
                assertIs<AppResult.Success<List<Arrival>>>(first)
                assertEquals(emptyList(), first.value)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun lb105_aCancelledCallIsMarkedCancelled() =
        runTest {
            // The capture has none: the source's own `cancellation` flag is what the row strikes through.
            repository(
                body = envelope(call(cancellation = true)),
            ).observeLineArrivals(lineId = 1, direction = null).test {
                val first = awaitItem()
                assertIs<AppResult.Success<List<Arrival>>>(first)
                assertTrue(first.value.single().isCancelled)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun lb105_anInaccuratePredictionIsLowConfidence() =
        runTest {
            // DD-09: every call of the capture carries predictionInaccurate: true, so all of them are low confidence.
            repository().observeLineArrivals(lineId = 1, direction = null).test {
                val first = awaitItem()
                assertIs<AppResult.Success<List<Arrival>>>(first)
                assertTrue(first.value.all { it.isLowConfidence })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun lb105_anUnparseableArrivalTimeDropsThatCallOnly() =
        runTest {
            val body =
                envelope(
                    call(expected = "2026-11-08T11:04:47+01:00"),
                    call(expected = "not a time"),
                    call(expected = "2026-11-08T11:09:47+01:00"),
                )

            repository(body = body).observeLineArrivals(lineId = 1, direction = null).test {
                val first = awaitItem()
                assertIs<AppResult.Success<List<Arrival>>>(first)
                assertEquals(2, first.value.size)
                assertTrue(logged.any { "Dropped a call without its time" in it }, logged.toString())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun lb105_aFailingCallKeepsTheErrorForTheScreen() =
        runTest {
            val failing =
                object : PollingEngine {
                    override fun <T> poll(
                        key: PollKey,
                        interval: Duration,
                        fetch: suspend () -> AppResult<T>,
                    ): Flow<Timed<T>> = flowOf(Timed(value = null, fetchedAt = START, lastError = AppError.Network))
                }
            val repository =
                ArrivalsRepositoryImpl(
                    api = RealtimeApi(client("{}"), logger),
                    polling = failing,
                    interval = INTERVAL,
                    previewMinutes = PREVIEW_MINUTES,
                    logger = logger,
                )

            repository.observeLineArrivals(lineId = 1, direction = null).test {
                assertEquals(AppResult.Failure(AppError.Network), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun lb105_anOpaqueDirectionCodeIsInferredFromWhereItsCallsAre() =
        runTest {
            // DD-04: "401" says nothing on its own; its calls do. Four calls at orders 1 to 4 at the outbound
            // pattern's own first four stops fit it and nothing of the inbound one, so they decide it.
            val body =
                envelope(
                    call(stop = 1, order = 1, direction = "401"),
                    call(stop = 2, order = 2, direction = "401"),
                    call(stop = 3, order = 3, direction = "401"),
                    call(stop = 4, order = 4, direction = "401"),
                    call(stop = 5, order = 1, direction = "402"),
                    call(stop = 6, order = 2, direction = "402"),
                    call(stop = 7, order = 3, direction = "402"),
                    call(stop = 8, order = 4, direction = "402"),
                )
            val patterns =
                mapOf(
                    Direction.OUTBOUND to listOf(1, 2, 3, 4),
                    Direction.INBOUND to listOf(5, 6, 7, 8),
                )

            repository(
                body = body,
                patterns = patterns,
            ).observeLineArrivals(lineId = 1, direction = Direction.OUTBOUND).test {
                val first = awaitItem()
                assertIs<AppResult.Success<List<Arrival>>>(first)
                assertEquals(
                    listOf(1, 2, 3, 4),
                    first.value.map { it.stopId }.sorted(),
                    "the 401 calls, placed on the outbound pattern, and not the 402 ones",
                )
                assertTrue(first.value.all { it.direction == Direction.OUTBOUND })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun lb105_withoutThePatternsNoCallIsAttributedToADirection() =
        runTest {
            // The honest fallback: an empty pattern map decides nothing, so asking for a direction finds nothing
            // rather than putting a bus on the wrong one.
            repository().observeLineArrivals(lineId = 1, direction = Direction.OUTBOUND).test {
                assertEquals(AppResult.Success(emptyList<Arrival>()), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun lb105_anEmptyExpectedTimeFallsBackToTheAimedOne_ratherThanDroppingTheCall() =
        runTest {
            // DD-07: the source writes "" for a time it does not have.
            val body =
                envelope(
                    """
                    {"lineRef":"1","directionRef":"401","stopPointRef":"38","order":25,
                     "expectedArrivalTime":"","aimedArrivalTime":"2026-11-08T11:04:47+01:00"}
                    """.trimIndent(),
                )

            repository(body = body).observeLineArrivals(lineId = 1, direction = null).test {
                val first = awaitItem()
                assertIs<AppResult.Success<List<Arrival>>>(first)
                assertEquals(1, first.value.size, "the aimed time is a valid fallback")
                assertEquals(Instant.parse("2026-11-08T11:04:47+01:00"), first.value.single().expectedAt)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun lb105_aFailureAfterASuccess_isAFailure_notTheStaleCalls() =
        runTest {
            // The engine keeps the last good value beside the error. A list of ETAs from the last poll re-worded as
            // "Now" is worse than none, so the screen drops them and shows Retry (FR-ERR-05).
            val stale =
                object : PollingEngine {
                    override fun <T> poll(
                        key: PollKey,
                        interval: Duration,
                        fetch: suspend () -> AppResult<T>,
                    ): Flow<Timed<T>> =
                        flow {
                            // The engine's own rule: it keeps the last good value and sets the error beside it.
                            val call = fetch()
                            val value = (call as? AppResult.Success)?.value
                            emit(Timed(value, START, lastError = AppError.Network))
                        }
                }
            val repository =
                ArrivalsRepositoryImpl(
                    api = RealtimeApi(client(FixtureLoader.read("et_line1_60min.json")), logger),
                    polling = stale,
                    interval = INTERVAL,
                    previewMinutes = PREVIEW_MINUTES,
                    logger = logger,
                )

            repository.observeLineArrivals(lineId = 1, direction = null).test {
                assertEquals(AppResult.Failure(AppError.Network), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun call(
        expected: String = "2026-11-08T11:04:47+01:00",
        cancellation: Boolean = false,
    ) = call(stop = 38, order = 25, direction = "401", expected = expected, cancellation = cancellation)

    private fun call(
        stop: Int,
        order: Int,
        direction: String,
        expected: String = "2026-11-08T11:04:47+01:00",
        cancellation: Boolean = false,
    ) = """
        {"lineRef":"1","directionRef":"$direction","stopPointRef":"$stop","order":$order,
         "expectedArrivalTime":"$expected","cancellation":$cancellation,"predictionInaccurate":true}
        """.trimIndent()

    /** The BFF envelope of api_spec.md §3: every source response is an object under `result`. */
    private fun envelope(vararg calls: String) = """{"result":{"arrivals":[${calls.joinToString(",")}]}}"""

    private companion object {
        const val BASE_URL = "https://transporteurbano.logrono.es"
        const val PREVIEW_MINUTES = 60

        // Line 1 outbound's 25th stop is 38 (spec §4.6's example), which places the 401 calls.
        const val ORDER_OF_38 = 25
        val INTERVAL = 20.seconds
    }
}

/** A clock stopped where the capture starts, so a request's Date header cannot move anything. */
private fun stoppedClock() =
    com.ruizurraca.logrobus.core.common.ServerCorrectedClock(
        object : kotlin.time.Clock {
            override fun now(): kotlin.time.Instant = START
        },
    )

private val START = kotlin.time.Instant.parse("2026-11-08T10:00:00Z")
