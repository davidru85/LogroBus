package com.ruizurraca.logrobus.android.feature.planner

import androidx.compose.material3.SelectableDates
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * FR-PLAN-03: the date picker offers [first], today on the Madrid clock, and the days after it, so no trip departs
 * on a day gone by. Material's picker counts its days as UTC midnights.
 */
internal class DaysFrom(
    private val first: LocalDate,
) : SelectableDates {
    private val firstMillis = first.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= firstMillis

    override fun isSelectableYear(year: Int): Boolean = year >= first.year
}
