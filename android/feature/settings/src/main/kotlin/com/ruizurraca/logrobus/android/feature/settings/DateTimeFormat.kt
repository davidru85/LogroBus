package com.ruizurraca.logrobus.android.feature.settings

import android.icu.text.SimpleDateFormat
import android.icu.util.GregorianCalendar
import android.icu.util.TimeZone
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.ruizurraca.logrobus.core.presentation.MadridDateTime

/** Formats a Madrid wall-clock time in the device locale, as a medium date and a time in the device's 12/24-h style. */
internal class DateTimeFormat(
    pattern: String,
    locale: java.util.Locale,
) {
    // The fields are already on the Madrid clock (AGENTS.md §5.5), so one fixed zone carries them through unchanged.
    private val formatter = SimpleDateFormat(pattern, locale).apply { timeZone = TimeZone.GMT_ZONE }

    fun format(time: MadridDateTime): String {
        val calendar = GregorianCalendar(TimeZone.GMT_ZONE)
        calendar.clear()
        calendar.set(time.year, time.month - 1, time.day, time.hour, time.minute)
        return formatter.format(calendar.time)
    }
}

@Composable
internal fun rememberDateTimeFormat(): DateTimeFormat {
    val locale = LocalConfiguration.current.locales[0]
    val twentyFourHour = DateFormat.is24HourFormat(LocalContext.current)
    return remember(locale, twentyFourHour) {
        DateTimeFormat(DateFormat.getBestDateTimePattern(locale, if (twentyFourHour) "yMMMdHm" else "yMMMdhm"), locale)
    }
}
