package com.ruizurraca.logrobus.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

/** Spec §8.6: the `Date` header that corrects the app clock. */
class HttpDateTest {
    @Test
    fun lb005_anImfFixdateIsReadAsAnInstant() {
        val dates =
            mapOf(
                "Thu, 01 Jan 1970 00:00:00 GMT" to Instant.fromEpochSeconds(0),
                "Sun, 06 Nov 1994 08:49:37 GMT" to Instant.parse("1994-11-06T08:49:37Z"),
                "Thu, 29 Feb 2024 23:59:59 GMT" to Instant.parse("2024-02-29T23:59:59Z"),
                "Sun, 29 Mar 2026 01:00:00 GMT" to Instant.parse("2026-03-29T01:00:00Z"),
                "Tue, 27 Oct 2026 08:00:30 GMT" to Instant.parse("2026-10-27T08:00:30Z"),
                "Fri, 31 Dec 2100 23:59:59 GMT" to Instant.parse("2100-12-31T23:59:59Z"),
            )

        assertEquals(dates, dates.mapValues { (text, _) -> parseHttpDate(text) })
    }

    @Test
    fun lb005_anyOtherDateTextIsIgnored() {
        // The BFF sends IMF-fixdate (RFC 9110 §5.6.7); the obsolete RFC 850 and asctime forms are not read.
        val unreadable =
            listOf(
                "",
                "yesterday",
                "Tue, 31 Feb 2026 08:00:30 GMT",
                "Sun, 29 Feb 2026 08:00:30 GMT",
                "Tue, 27 Oct 2026 24:00:00 GMT",
                "Tue, 27 Oct 2026 08:60:00 GMT",
                "Tue, 27 Okt 2026 08:00:30 GMT",
                "Tue, 27 Oct 2026 08:00:30 CET",
                "Tuesday, 27-Oct-26 08:00:30 GMT",
                "Tue Oct 27 08:00:30 2026",
            )

        assertEquals(unreadable.map { null }, unreadable.map(::parseHttpDate))
        assertNull(parseHttpDate("Tue, 27 Oct 2026 08:00:30 GMT trailing"))
    }
}
