package com.ruizurraca.logrobus.android.feature.planner

import android.content.Context
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.logrobus.core.presentation.MadridDateTime
import com.ruizurraca.logrobus.feature.planner.presentation.DepartureDay
import com.ruizurraca.logrobus.feature.planner.presentation.DeparturePickerUi
import com.ruizurraca.logrobus.feature.planner.presentation.DepartureUi
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerEvent
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * FR-PLAN-03 on Android: the departure chip under the fields says "Leave now" or the picked time, and its menu leaves
 * now or opens Material's date picker, then its time picker, which send the picked date and time on the Madrid clock.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlannerDepartureTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val events = mutableListOf<PlannerEvent>()

    @Test
    fun lb303_theDepartureChip_isAButton_sayingLeaveNow() {
        show(PlannerPreviewData.myLocation)

        compose
            .onNode(hasText(context.getString(R.string.plan_leave_now)) and !hasAnyAncestor(isPopup()))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    @Config(qualifiers = "es")
    fun lb303_aTimeToday_saysTheTime() {
        show(departingAt(MadridDateTime(2026, 10, 22, 8, 19), DepartureDay.TODAY))

        compose.onNodeWithText("Salir a las 08:19").assertExists()
    }

    @Test
    @Config(qualifiers = "es")
    fun lb303_aTimeInTheOneOClockHour_takesTheSingularArticle_inSpanish() {
        show(departingAt(MadridDateTime(2026, 10, 22, 1, 5), DepartureDay.TODAY))

        compose.onNodeWithText("Salir a la 01:05").assertExists()
    }

    @Test
    @Config(qualifiers = "es")
    fun lb303_aTimeTomorrow_saysTomorrow() {
        show(departingAt(MadridDateTime(2026, 10, 23, 8, 19), DepartureDay.TOMORROW))

        compose.onNodeWithText("Salir mañana a las 08:19").assertExists()
    }

    @Test
    @Config(qualifiers = "es")
    fun lb303_aTimeOnAnotherDay_saysItsDate() {
        show(departingAt(MadridDateTime(2027, 1, 15, 8, 19), DepartureDay.OTHER))

        compose.onNodeWithText("Salir el 15 ene a las 08:19").assertExists()
    }

    @Test
    @Config(qualifiers = "en")
    fun lb303_inEnglish_aTimeTomorrow_saysDepartTomorrow() {
        show(departingAt(MadridDateTime(2026, 10, 23, 8, 19), DepartureDay.TOMORROW))

        compose.onNodeWithText("Depart tomorrow at 08:19").assertExists()
    }

    @Test
    fun lb303_theChipsMenu_leavesNow_orOpensThePickers() {
        show(departingAt(MadridDateTime(2026, 10, 22, 8, 19), DepartureDay.TODAY))

        openMenu(R.plurals.plan_depart_today, quantity = 8, "08:19")
        compose.onNode(hasText(context.getString(R.string.plan_leave_now)) and hasAnyAncestor(isPopup())).performClick()
        openMenu(R.plurals.plan_depart_today, quantity = 8, "08:19")
        compose
            .onNode(
                hasText(context.getString(R.string.plan_depart_at_choose)) and hasAnyAncestor(isPopup()),
            ).performClick()

        assertEquals(listOf(PlannerEvent.LeaveNow, PlannerEvent.ChooseDepartureTime), events)
    }

    @Test
    fun lb303_thePickers_offerTheDate_thenTheTime_andSendThePickedOnes() {
        show(picking(start = MadridDateTime(2026, 10, 23, 8, 19)))

        compose.onNodeWithText(context.getString(R.string.plan_departure_ok)).performClick()
        compose.onNodeWithText(context.getString(R.string.plan_departure_ok)).performClick()

        assertEquals(listOf(PlannerEvent.DepartAt(year = 2026, month = 10, day = 23, hour = 8, minute = 19)), events)
    }

    @Test
    fun lb303_cancellingTheDate_sendsCancel() {
        show(picking(start = MadridDateTime(2026, 10, 23, 8, 19)))

        compose.onNodeWithText(context.getString(R.string.plan_departure_cancel)).performClick()

        assertEquals(listOf<PlannerEvent>(PlannerEvent.CancelDepartureTime), events)
    }

    @Test
    fun lb303_cancellingTheTime_sendsCancel() {
        show(picking(start = MadridDateTime(2026, 10, 23, 8, 19)))

        compose.onNodeWithText(context.getString(R.string.plan_departure_ok)).performClick()
        compose.onNodeWithText(context.getString(R.string.plan_departure_cancel)).performClick()

        assertEquals(listOf<PlannerEvent>(PlannerEvent.CancelDepartureTime), events)
    }

    @Test
    fun lb303_noDayBeforeToday_canBePicked() {
        val today = LocalDate.of(2026, 10, 22)
        val days = DaysFrom(today)

        assertFalse(days.isSelectableDate(today.minusDays(1).utcMillis()))
        assertTrue(days.isSelectableDate(today.utcMillis()))
        assertTrue(days.isSelectableDate(today.plusYears(1).utcMillis()))
        assertFalse(days.isSelectableYear(2025))
        assertTrue(days.isSelectableYear(2026))
    }

    private fun LocalDate.utcMillis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun departingAt(
        time: MadridDateTime,
        day: DepartureDay,
    ) = PlannerPreviewData.myLocation.copy(departure = DepartureUi.At(time, day))

    // The Madrid clock stands at 07:00 on 22 Oct 2026.
    private fun picking(start: MadridDateTime) =
        PlannerPreviewData.myLocation.copy(
            departure = DepartureUi.At(start, DepartureDay.TOMORROW),
            departurePicker = DeparturePickerUi(start = start, now = MadridDateTime(2026, 10, 22, 7, 0)),
        )

    private fun openMenu(
        chip: Int,
        quantity: Int,
        time: String,
    ) {
        val text = context.resources.getQuantityString(chip, quantity, time)
        compose.onNode(hasText(text) and !hasAnyAncestor(isPopup())).performClick()
    }

    private fun show(state: PlannerUiState) {
        compose.setContent { PlannerScreen(state, onEvent = { events += it }, onOpenMore = {}) }
    }
}
