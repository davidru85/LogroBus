package com.ruizurraca.logrobus.android.feature.planner

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.ruizurraca.logrobus.feature.planner.presentation.DepartureUi
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerEvent

/**
 * FR-PLAN-03: "Leave now" or the picked time (design_guidelines.md §3.3). Its menu leaves now, or opens the date and
 * time pickers.
 */
@Composable
internal fun DepartureChip(
    departure: DepartureUi,
    onEvent: (PlannerEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menu by rememberSaveable { mutableStateOf(false) }
    // The menu opens from the chip.
    Box(modifier) {
        AssistChip(
            onClick = { menu = true },
            label = { Text(departureText(departure)) },
            // Decoration: it says a menu opens.
            trailingIcon = {
                Icon(
                    painterResource(R.drawable.ic_arrow_drop_down),
                    contentDescription = null,
                    Modifier.size(AssistChipDefaults.IconSize),
                )
            },
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            MenuItem(R.string.plan_leave_now) {
                menu = false
                onEvent(PlannerEvent.LeaveNow)
            }
            MenuItem(R.string.plan_depart_at_choose) {
                menu = false
                onEvent(PlannerEvent.ChooseDepartureTime)
            }
        }
    }
}

@Composable
private fun MenuItem(
    @StringRes text: Int,
    onClick: () -> Unit,
) {
    DropdownMenuItem(text = { Text(stringResource(text)) }, onClick = onClick)
}
