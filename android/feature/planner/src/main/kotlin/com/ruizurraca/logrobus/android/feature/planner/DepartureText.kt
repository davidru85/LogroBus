package com.ruizurraca.logrobus.android.feature.planner

import android.icu.text.SimpleDateFormat
import android.icu.util.GregorianCalendar
import android.icu.util.TimeZone
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.ruizurraca.logrobus.android.core.designsystem.clockText
import com.ruizurraca.logrobus.core.presentation.MadridDateTime
import com.ruizurraca.logrobus.feature.planner.presentation.DepartureDay
import com.ruizurraca.logrobus.feature.planner.presentation.DepartureUi
import java.util.Locale

/**
 * FR-PLAN-03: what the departure chip says: "Leave now", or the picked time, "Depart at 08:19", "Depart tomorrow at
 * 08:19" or "Depart on 15 Jan at 08:19". The hour picks Spanish's article: "a la 01:05", "a las 08:19".
 */
@Composable
internal fun departureText(departure: DepartureUi): String =
    when (departure) {
        DepartureUi.Now -> {
            stringResource(R.string.plan_leave_now)
        }

        is DepartureUi.At -> {
            val time = departure.time
            // HH:mm on the Madrid clock, as the ETAs' clock times (spec §5.7).
            val clock = clockText(time.hour, time.minute)
            when (departure.relativeDay) {
                DepartureDay.TODAY -> {
                    pluralStringResource(R.plurals.plan_depart_today, time.hour, clock)
                }

                DepartureDay.TOMORROW -> {
                    pluralStringResource(R.plurals.plan_depart_tomorrow, time.hour, clock)
                }

                DepartureDay.OTHER -> {
                    pluralStringResource(R.plurals.plan_depart_on, time.hour, rememberDayFormat().format(time), clock)
                }
            }
        }
    }

/** A day and its month in the app's locale: "15 ene", "Jan 15". */
private class DayFormat(
    locale: Locale,
) {
    // The fields are already on the Madrid clock (AGENTS.md §5.5), so one fixed zone carries them through unchanged.
    private val formatter =
        SimpleDateFormat(
            DateFormat.getBestDateTimePattern(locale, "dMMM"),
            locale,
        ).apply { timeZone = TimeZone.GMT_ZONE }

    fun format(time: MadridDateTime): String {
        val calendar = GregorianCalendar(TimeZone.GMT_ZONE)
        calendar.clear()
        calendar.set(time.year, time.month - 1, time.day)
        return formatter.format(calendar.time)
    }
}

@Composable
private fun rememberDayFormat(): DayFormat {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { DayFormat(locale) }
}
