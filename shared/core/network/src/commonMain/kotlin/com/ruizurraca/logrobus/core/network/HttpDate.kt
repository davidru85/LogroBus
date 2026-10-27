package com.ruizurraca.logrobus.core.network

import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** RFC 9110 §5.6.7: the form servers send, `Sun, 06 Nov 1994 08:49:37 GMT`. The obsolete forms are not read. */
private val IMF_FIXDATE = Regex("""[A-Z][a-z]{2}, (\d{2}) ([A-Z][a-z]{2}) (\d{4}) (\d{2}):(\d{2}):(\d{2}) GMT""")

/**
 * No server sends an earlier time: LogroBus did not exist before it. A clock that was never set, such as a proxy's
 * at 1970, would otherwise move the app clock by decades. A device clock that is wrong is still corrected.
 */
private val EARLIEST_SERVER_TIME = Instant.parse("2026-01-01T00:00:00Z")

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/**
 * The server's time when it sent the response with these [headers], or null when they cannot tell. A cache keeps the
 * origin's `Date` and adds `Age`, the seconds since then (RFC 9111 §5.1), so the server's time is their sum.
 */
internal fun serverNow(headers: Headers): Instant? {
    val date = headers[HttpHeaders.Date]?.let(::parseHttpDate) ?: return null
    val age = headers[HttpHeaders.Age]?.let { it.toLongOrNull()?.takeIf { seconds -> seconds >= 0 } ?: return null }
    return (date + (age ?: 0).seconds).takeIf { it >= EARLIEST_SERVER_TIME }
}

/** The instant of a `Date` header [value], or null when it cannot be read: a bad `Date` is ignored, never an error. */
internal fun parseHttpDate(value: String): Instant? =
    IMF_FIXDATE
        .matchEntire(value)
        ?.groupValues
        ?.let(DateFields::of)
        ?.takeIf { it.isValid }
        ?.toInstant()

/** A UTC date and time as a `Date` header writes it; [month] counts from 1. */
@Suppress("MagicNumber") // The calendar's own numbers.
private class DateFields(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Int,
) {
    // A server clock before 1970 is wrong whatever it says; the bound also keeps epochDay to positive years.
    val isValid: Boolean
        get() = year >= 1970 && month in 1..12 && day in 1..daysInMonth && hour < 24 && minute < 60 && second < 60

    private val daysInMonth: Int
        get() =
            when (month) {
                2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
                4, 6, 9, 11 -> 30
                else -> 31
            }

    fun toInstant(): Instant = Instant.fromEpochSeconds(epochDay() * 86_400 + hour * 3_600 + minute * 60 + second)

    companion object {
        /** From the groups of [IMF_FIXDATE]: day, month name, year, hour, minute and second. */
        fun of(groups: List<String>) =
            DateFields(
                year = groups[3].toInt(),
                month = MONTHS.indexOf(groups[2]) + 1,
                day = groups[1].toInt(),
                hour = groups[4].toInt(),
                minute = groups[5].toInt(),
                second = groups[6].toInt(),
            )
    }

    /** Days since 1970-01-01 in the Gregorian calendar, for positive years (H. Hinnant's `days_from_civil`). */
    private fun epochDay(): Long {
        val y = if (month <= 2) year - 1 else year
        val era = y / 400
        val yearOfEra = y - era * 400
        val dayOfYear = (153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1
        val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
        return era * 146_097L + dayOfEra - 719_468
    }
}
