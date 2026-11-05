package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.common.ServerCorrectedClock
import com.ruizurraca.logrobus.core.network.ClientIdentity
import com.ruizurraca.logrobus.core.network.RequestKind
import com.ruizurraca.logrobus.core.network.SourceClient
import com.ruizurraca.logrobus.core.preferences.EtaFormat
import com.ruizurraca.logrobus.core.presentation.EtaDisplay
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * FR-ERR-06 (LB-408): ETAs count from the server's clock, not the device's. The source client passes the `Date` of
 * every response to the app's one [ServerCorrectedClock] (spec §8.6), the graph's `AppClock` too, and an ETA is worked
 * out from that clock's now when it renders (spec §5.7).
 */
class ServerClockOffsetTest {
    /** The device clock runs 3 min ahead of the server's. */
    private val clock =
        ServerCorrectedClock(
            object : Clock {
                override fun now() = SERVER_NOW + 3.minutes
            },
        )

    private val client =
        SourceClient(
            engine = MockEngine { respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.Date, SERVER_DATE)) },
            identity = ClientIdentity(appVersion = "1.0.0", platform = "Android", osVersion = "16"),
            clock = clock,
            baseUrl = { "https://transporteurbano.logrono.es" },
            logger = appLogger(debug = true) {},
        )

    @Test
    fun lb408_withTheDeviceClock3MinutesAhead_anArrival5MinutesAwayShows5Minutes() =
        runTest {
            fetchArrivals()

            assertEquals(EtaDisplay.Minutes(5), eta())
        }

    @Test
    fun lb408_untilTheFirstServerDateTheDeviceClockCounts_andTheFirstCorrectsTheNextEta() =
        runTest {
            assertEquals(EtaDisplay.Minutes(2), eta())

            fetchArrivals()

            assertEquals(EtaDisplay.Minutes(5), eta())
        }

    /** A stop's arrivals (API-06), whose response carries the server's clock. */
    private suspend fun fetchArrivals() {
        client.get("/api/estimatedTimetable/byStop/38", RequestKind.Live, mapOf("lines" to "1,3"))
    }

    /** How an arrival the server expects 5 min after [SERVER_NOW] shows when it renders, in minutes. */
    private fun eta() = EtaDisplay.of(expected = SERVER_NOW + 5.minutes, now = clock.now(), format = EtaFormat.MINUTES)

    private companion object {
        val SERVER_NOW = Instant.parse("2026-11-05T08:00:00Z")

        /** [SERVER_NOW] as the server's `Date` header gives it. */
        const val SERVER_DATE = "Thu, 05 Nov 2026 08:00:00 GMT"
    }
}
