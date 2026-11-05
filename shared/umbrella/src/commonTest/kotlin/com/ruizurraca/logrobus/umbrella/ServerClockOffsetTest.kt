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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * FR-ERR-06 (LB-408): ETAs count from the server's clock, not the device's. The source client passes the `Date` of
 * every response to a [ServerCorrectedClock] (spec §8.6), and an ETA counts from that clock's now (spec §5.7). This
 * test wires those pieces by hand. The graph binds one clock as the `AppClock` (`GraphAssertions`); the arrival rows
 * that will read it, and a test through the graph, come with LB-205.
 */
class ServerClockOffsetTest {
    @Test
    fun lb408_withTheDeviceClock3MinutesAhead_anArrival5MinutesAwayShows5Minutes() =
        runTest {
            val app = App(deviceClockAhead = 3.minutes)

            app.fetchArrivals()

            assertEquals(EtaDisplay.Minutes(5), app.eta(IN_5_MINUTES))
        }

    @Test
    fun lb408_untilTheFirstServerDateTheDeviceClockCounts_andTheFirstCorrectsTheNextEta() =
        runTest {
            val app = App(deviceClockAhead = 3.minutes)
            assertEquals(EtaDisplay.Minutes(2), app.eta(IN_5_MINUTES))

            app.fetchArrivals()

            assertEquals(EtaDisplay.Minutes(5), app.eta(IN_5_MINUTES))
        }

    @Test
    fun lb408_anOffsetUnder60SecondsCorrectsTheEtaToo() =
        runTest {
            // FR-ERR-06 requires the offset beyond 60 s, and spec §8.6 applies it at any size: 50 s would still show
            // an arrival 4 min 30 s away as 3 min.
            val app = App(deviceClockAhead = 50.seconds)
            assertEquals(EtaDisplay.Minutes(3), app.eta(4.minutes + 30.seconds))

            app.fetchArrivals()

            assertEquals(EtaDisplay.Minutes(4), app.eta(4.minutes + 30.seconds))
        }

    /** The app's clock and source client, on a device clock [deviceClockAhead] of the server's. */
    private class App(
        deviceClockAhead: Duration,
    ) {
        private val clock =
            ServerCorrectedClock(
                object : Clock {
                    override fun now() = SERVER_NOW + deviceClockAhead
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

        /** A stop's arrivals (API-06), whose response carries the server's clock. */
        suspend fun fetchArrivals() {
            client.get("/api/estimatedTimetable/byStop/38", RequestKind.Live, mapOf("lines" to "1,3"))
        }

        /** How an arrival the server expects [away] from [SERVER_NOW] shows when it renders, in minutes. */
        fun eta(away: Duration) = EtaDisplay.of(expected = SERVER_NOW + away, now = clock.now(), EtaFormat.MINUTES)
    }

    private companion object {
        val SERVER_NOW = Instant.parse("2026-11-05T08:00:00Z")

        /** [SERVER_NOW] as the server's `Date` header gives it. */
        const val SERVER_DATE = "Thu, 05 Nov 2026 08:00:00 GMT"

        /**
         * "In 5 min" on the screen: half a minute past the boundary, since an ETA is rounded down, so a correction of a
         * few seconds either way does not change it.
         */
        val IN_5_MINUTES = 5.minutes + 30.seconds
    }
}
