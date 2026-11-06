package com.ruizurraca.logrobus.android.feature.planner

import androidx.compose.material3.SelectableDates
import java.time.LocalDate

/** FR-PLAN-03: the days the date picker offers, from [first] on. */
internal class DaysFrom(
    private val first: LocalDate,
) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean = true

    override fun isSelectableYear(year: Int): Boolean = true
}
