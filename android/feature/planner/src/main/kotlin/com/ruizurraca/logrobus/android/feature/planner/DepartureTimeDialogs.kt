package com.ruizurraca.logrobus.android.feature.planner

import android.text.format.DateFormat
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.getSelectedDate
import androidx.compose.material3.isInputValid
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import com.ruizurraca.logrobus.core.presentation.MadridDateTime
import com.ruizurraca.logrobus.feature.planner.presentation.DeparturePickerUi
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerEvent
import java.time.LocalDate

/**
 * FR-PLAN-03: Material's date picker, then its time picker, open at [picker]'s start on the Madrid clock
 * (design_guidelines.md §3.3). No day before today can be picked; any time can. OK on the time sends the picked date
 * and time through [onPicked]; Cancel, back or a tap outside either dialog calls [onCancel].
 */
@Composable
internal fun DepartureTimeDialogs(
    picker: DeparturePickerUi,
    onPicked: (PlannerEvent.DepartAt) -> Unit,
    onCancel: () -> Unit,
) {
    // The date OK'd in the first dialog; the time's dialog shows once there is one.
    var date by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    val picked = date
    if (picked == null) {
        DateDialog(picker, onDate = { date = it }, onCancel = onCancel)
    } else {
        TimeDialog(
            start = picker.start,
            onTime = { hour, minute ->
                onPicked(PlannerEvent.DepartAt(picked.year, picked.monthValue, picked.dayOfMonth, hour, minute))
            },
            onCancel = onCancel,
        )
    }
}

@Composable
private fun DateDialog(
    picker: DeparturePickerUi,
    onDate: (LocalDate) -> Unit,
    onCancel: () -> Unit,
) {
    val today = picker.now.date()
    val days = remember(today) { DaysFrom(today) }
    val state =
        rememberDatePickerState(
            initialSelectedDate = picker.start.date(),
            yearRange = today.year..DatePickerDefaults.YearRange.last,
            selectableDates = days,
        )
    DatePickerDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(onClick = { state.getSelectedDate()?.let(onDate) }, enabled = state.selectedDateMillis != null) {
                Text(stringResource(R.string.plan_departure_ok))
            }
        },
        dismissButton = { CancelButton(onCancel) },
    ) { DatePicker(state) }
}

@Composable
private fun TimeDialog(
    start: MadridDateTime,
    onTime: (hour: Int, minute: Int) -> Unit,
    onCancel: () -> Unit,
) {
    val state =
        rememberTimePickerState(
            initialHour = start.hour,
            initialMinute = start.minute,
            is24Hour = DateFormat.is24HourFormat(LocalContext.current),
        )
    // The clock face needs room: on a short window, as in landscape, only the keyboard's fields fit.
    val height =
        with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.height
                .toDp()
        }
    val roomy = height > TimePickerDialogDefaults.MinHeightForTimePicker
    var typing by rememberSaveable { mutableStateOf(false) }
    val mode = if (typing || !roomy) TimePickerDisplayMode.Input else TimePickerDisplayMode.Picker
    TimePickerDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(onClick = { onTime(state.hour, state.minute) }, enabled = state.isInputValid) {
                Text(stringResource(R.string.plan_departure_ok))
            }
        },
        title = { TimePickerDialogDefaults.Title(displayMode = mode) },
        modeToggleButton = {
            if (roomy) TimePickerDialogDefaults.DisplayModeToggle(onDisplayModeChange = { typing = !typing }, mode)
        },
        dismissButton = { CancelButton(onCancel) },
    ) {
        if (mode == TimePickerDisplayMode.Input) TimeInput(state) else TimePicker(state)
    }
}

@Composable
private fun CancelButton(onCancel: () -> Unit) {
    TextButton(onClick = onCancel) { Text(stringResource(R.string.plan_departure_cancel)) }
}

private fun MadridDateTime.date(): LocalDate = LocalDate.of(year, month, day)
