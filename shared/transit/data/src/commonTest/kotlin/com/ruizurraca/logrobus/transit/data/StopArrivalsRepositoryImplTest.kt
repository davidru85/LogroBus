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
import com.ruizurraca.logrobus.transit.domain.Direction
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * LB-205 (FR-STOP-03 to FR-STOP-05): the calls at a stop come from API-06 for the lines it serves, within the 60 min
 * FR-STOP-03 fixes, and the answer carries the instant it arrived so the screen can say how old it is (FR-ERR-06).
 */
class StopArrivalsRepositoryImplTest {
    private val recordedUrls = mutableListOf<String>()

    private val logger =
        object : Logger {
            override fun log(
                level: LogLevel,
                tag: String,
                message: String,
                throwable: Throwable?,
            ) = Unit
        }

    private val START = Instant.parse("2026-11-08T10:00:00Z")

    private fun fakePolling() =
        object : PollingEngine {
            override fun <T> poll(
                key: PollKey,
                interval: Duration,
                fetch: suspend () -> AppResult<T>,
            ): Flow<Timed<T>> =
                flow {
                    when (val result = fetch()) {
                        is AppResult.Success -> emit(Timed(value = result.value, fetchedAt = START, lastError = null))
                        is AppResult.Failure -> emit(Timed(value = null, fetchedAt = START, lastError = result.error))
                    }
                }
        }

    private fun reader(body: String = FixtureLoader.read("et_stop38.json")) =
        StopArrivalsRepositoryImpl(
            api = RealtimeApi(client(body), logger),
            polling = fakePolling(),
            interval = 20.seconds,
            previewMinutes = PREVIEW_MINUTES,
            logger = logger,
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
    fun lb205_api06IsAskedForTheStopsLinesWithinSixtyMinutes() =
        runTest {
            reader().observeStopArrivals(stopId = 38, lineIds = listOf(1, 33)).test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            val url = recordedUrls.single()
            assertTrue(url.contains("/api/estimatedTimetable/byStop/38"), url)
            assertTrue(url.contains("lines=1,33"), url)
            assertTrue(url.contains("previewMinutes=$PREVIEW_MINUTES"), url)
        }

    @Test
    fun lb205_theCaptureYieldsItsTwoCallsAndTheInstantTheAnswerArrived() =
        runTest {
            reader().observeStopArrivals(stopId = 38, lineIds = listOf(1)).test {
                val first = awaitItem()
                val answer = assertIs<AppResult.Success<StopCallsAnswer>>(first).value
                // jq: two calls, both line 1 at stop 38, both flagged `predictionInaccurate`.
                assertEquals(2, answer.arrivals.size)
                assertTrue(answer.arrivals.all { it.lineId == 1 && it.stopId == 38 })
                assertTrue(answer.arrivals.all { it.isLowConfidence }, "DD-09: the capture flags both")
                assertEquals(START, answer.fetchedAt, "FR-ERR-06: the age is measured from the answer")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun lb205_aFailureAfterASuccessIsAFailureNotTheStaleCalls() =
        runTest {
            val stale =
                object : PollingEngine {
                    override fun <T> poll(
                        key: PollKey,
                        interval: Duration,
                        fetch: suspend () -> AppResult<T>,
                    ): Flow<Timed<T>> =
                        flow {
                            val call = fetch()
                            emit(Timed((call as? AppResult.Success)?.value, START, lastError = AppError.Network))
                        }
                }
            val reader =
                StopArrivalsRepositoryImpl(
                    api = RealtimeApi(client(FixtureLoader.read("et_stop38.json")), logger),
                    polling = stale,
                    interval = 20.seconds,
                    previewMinutes = PREVIEW_MINUTES,
                    logger = logger,
                )

            reader.observeStopArrivals(stopId = 38, lineIds = listOf(1)).test {
                assertEquals(AppResult.Failure(AppError.Network), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun lb205_aCallWithoutATimeIsDroppedAndTheRestKept() =
        runTest {
            val body =
                """{"result":{"arrivals":[
                    {"lineRef":"1","stopPointRef":"38","order":25,"expectedArrivalTime":"2026-11-08T11:04:47+01:00"},
                    {"lineRef":"1","stopPointRef":"38","order":25,"expectedArrivalTime":"not a time"}
                ]}}"""
            reader(body = body).observeStopArrivals(stopId = 38, lineIds = listOf(1)).test {
                val answer = assertIs<AppResult.Success<StopCallsAnswer>>(awaitItem()).value
                assertEquals(1, answer.arrivals.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private companion object {
        const val BASE_URL = "https://transporteurbano.logrono.es"
        const val PREVIEW_MINUTES = 60
    }
}

/** A clock stopped where the capture starts, so a request's Date header cannot move anything. */
private fun stoppedClock() =
    com.ruizurraca.logrobus.core.common.ServerCorrectedClock(
        object : kotlin.time.Clock {
            override fun now(): kotlin.time.Instant = Instant.parse("2026-11-08T10:00:00Z")
        },
    )
