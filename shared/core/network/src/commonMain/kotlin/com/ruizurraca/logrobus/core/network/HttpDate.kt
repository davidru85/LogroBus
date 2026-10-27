package com.ruizurraca.logrobus.core.network

import kotlin.time.Instant

/** RFC 9110 §5.6.7: the form servers send, `Sun, 06 Nov 1994 08:49:37 GMT`. The obsolete forms are not read. */
private val IMF_FIXDATE = Regex("""[A-Z][a-z]{2}, (\d{2}) ([A-Z][a-z]{2}) (\d{4}) (\d{2}):(\d{2}):(\d{2}) GMT""")

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private const val SECONDS_PER_DAY = 86_400L
private const val SECONDS_PER_HOUR = 3_600
private const val SECONDS_PER_MINUTE = 60

/** The instant of a `Date` header [value], or null when it cannot be read: a bad `Date` is ignored, never an error. */
internal fun parseHttpDate(value: String): Instant? {
    val fields = IMF_FIXDATE.matchEntire(value)?.groupValues ?: return null
    val day = fields[1].toInt()
    val month = MONTHS.indexOf(fields[2]) + 1
    val year = fields[3].toInt()
    val (hour, minute, second) = fields.subList(4, 7).map(String::toInt)
    // A server clock before 1970 is wrong whatever it says; the check also keeps epochDay to positive years.
    val valid = year >= 1970 && month > 0 && day in 1..daysIn(month, year) && hour < 24 && minute < 60 && second < 60
    if (!valid) return null
    val seconds = hour * SECONDS_PER_HOUR + minute * SECONDS_PER_MINUTE + second
    return Instant.fromEpochSeconds(epochDay(year, month, day) * SECONDS_PER_DAY + seconds)
}

private fun daysIn(
    month: Int,
    year: Int,
): Int =
    when (month) {
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

/** Days since 1970-01-01 in the Gregorian calendar, for positive years (H. Hinnant's `days_from_civil`). */
@Suppress("MagicNumber") // The constants of the published algorithm.
private fun epochDay(
    year: Int,
    month: Int,
    day: Int,
): Long {
    val y = if (month <= 2) year - 1 else year
    val era = y / 400
    val yearOfEra = y - era * 400
    val dayOfYear = (153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1
    val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
    return era * 146_097L + dayOfEra - 719_468
}
