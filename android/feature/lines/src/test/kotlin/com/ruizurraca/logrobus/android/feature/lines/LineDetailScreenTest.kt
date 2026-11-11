package com.ruizurraca.logrobus.android.feature.lines

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextDecoration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.core.presentation.EtaDisplay
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailEvent
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailUi
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailUiState
import com.ruizurraca.logrobus.feature.lines.presentation.StopArrivalUi
import com.ruizurraca.logrobus.feature.lines.presentation.StopRowUi
import com.ruizurraca.logrobus.transit.domain.Direction
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import com.ruizurraca.logrobus.android.core.designsystem.R as DS

/**
 * LB-103 (FR-LINE-09): the line detail, until LB-104 adds its map, directions and stops. Its title names the line, its
 * body shows the badge and the name, and its toolbar has the favourite toggle.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LineDetailScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val line3 = LineDetailUi(3, "3", "Campillo - El Cubo", 0xFFF9A825, 0xFF000000, isFavourite = false)

    @Test
    fun lb103_theDetailIsTitledByItsLine_andShowsItsBadgeAndName() {
        compose.setContent {
            LineDetailScreen(LineDetailUiState.Content(line3, Direction.OUTBOUND, emptyList(), emptyList()), onEvent = {
            }, onBack = {})
        }

        compose.onNode(isHeading() and hasText(context.getString(R.string.line_detail_title, "3"))).assertExists()
        compose.onNode(hasContentDescription(context.getString(DS.string.line_badge, "3"))).assertExists()
        compose.onNodeWithText("Campillo - El Cubo").assertExists()
    }

    @Test
    fun lb103_theToolbarsToggleIsLabelledByTheLine_andSendsItsNewValue() {
        val events = mutableListOf<LineDetailEvent>()
        compose.setContent {
            LineDetailScreen(LineDetailUiState.Content(line3, Direction.OUTBOUND, emptyList(), emptyList()), onEvent = {
                events +=
                    it
            }, onBack = {})
        }

        compose.onNodeWithContentDescription(context.getString(R.string.lines_favourite_add, "3")).performClick()

        assertEquals(listOf(LineDetailEvent.SetFavourite(isFavourite = true)), events)
    }

    @Test
    fun lb103_aFavouriteLinesToggleRemovesIt() {
        val events = mutableListOf<LineDetailEvent>()
        val favourite =
            LineDetailUiState.Content(
                line3.copy(isFavourite = true),
                Direction.OUTBOUND,
                emptyList(),
                emptyList(),
            )
        compose.setContent { LineDetailScreen(favourite, onEvent = { events += it }, onBack = {}) }

        compose.onNodeWithContentDescription(context.getString(R.string.lines_favourite_remove, "3")).performClick()

        assertEquals(listOf(LineDetailEvent.SetFavourite(isFavourite = false)), events)
    }

    @Test
    fun lb103_upGoesBack() {
        var back = 0
        compose.setContent {
            LineDetailScreen(LineDetailUiState.Content(line3, Direction.OUTBOUND, emptyList(), emptyList()), onEvent = {
            }, onBack = { back++ })
        }

        compose.onNodeWithContentDescription(context.getString(DS.string.action_navigate_up)).performClick()

        assertEquals(1, back)
    }

    @Test
    fun lb103_theDetailHasATitleWhileItLoads_andAfterAnError() {
        // TalkBack meets a titled screen at once; the line's code comes with its data.
        var state: LineDetailUiState by mutableStateOf(LineDetailUiState.Loading)
        compose.setContent { LineDetailScreen(state, onEvent = {}, onBack = {}) }

        compose.onNode(isHeading() and hasText(context.getString(R.string.line_detail_title_generic))).assertExists()
        state = LineDetailUiState.Error(ErrorReason.NETWORK)
        compose.onNode(isHeading() and hasText(context.getString(R.string.line_detail_title_generic))).assertExists()
    }

    @Test
    fun lb103_theFirstLoadIsAnnouncedToTalkBack() {
        compose.setContent { LineDetailScreen(LineDetailUiState.Loading, onEvent = {}, onBack = {}) }

        compose.onNode(hasContentDescription(context.getString(R.string.line_detail_loading))).assertExists()
    }

    @Test
    fun lb103_anErrorShowsItsReason_andRetrySendsTheEvent() {
        val events = mutableListOf<LineDetailEvent>()
        compose.setContent {
            LineDetailScreen(LineDetailUiState.Error(ErrorReason.NETWORK), onEvent = { events += it }, onBack = {})
        }

        compose.onNodeWithText(context.getString(DS.string.error_network)).assertExists()
        compose.onNodeWithText(context.getString(DS.string.error_retry)).performClick()

        assertEquals(listOf(LineDetailEvent.Retry), events)
    }

    @Test
    fun lb105_eachStopRowShowsItsNextCall_orTheDash_andStrikesACancelledOne() {
        // FR-LINE-05 and design_guidelines.md §6.3: the design system draws the ETA, so the row is checked for what it
        // hands it, and for the dash of a stop whose call is null.
        val stops =
            listOf(
                StopRowUi(stopId = 38, order = 1, name = "Campillo", lat = 42.45, lng = -2.44),
                StopRowUi(stopId = 39, order = 2, name = "Varea", lat = 42.46, lng = -2.43),
                StopRowUi(stopId = 40, order = 3, name = "El Cubo", lat = 42.47, lng = -2.42),
            )
        val arrivals =
            mapOf(
                38 to StopArrivalUi(EtaDisplay.Minutes(4), isLowConfidence = false, isCancelled = false),
                39 to StopArrivalUi(EtaDisplay.Minutes(2), isLowConfidence = false, isCancelled = true),
                40 to StopArrivalUi(null, isLowConfidence = false, isCancelled = false),
            )
        compose.setContent {
            LineDetailScreen(
                LineDetailUiState.Content(line3, Direction.OUTBOUND, directions = emptyList(), stops = stops),
                onEvent = {},
                onBack = {},
                arrivals = arrivals,
            )
        }

        // Each row is one utterance: the stop and its call, never the ETA worded again.
        val fourMinutes = context.resources.getQuantityString(DS.plurals.eta_minutes_spoken, 4, 4)
        val cancelled = context.getString(DS.string.eta_cancelled_spoken)
        val noCalls = context.getString(R.string.line_detail_eta_none_spoken)
        compose.onNodeWithContentDescription(row("Campillo", 1, fourMinutes)).assertExists()
        compose.onNodeWithContentDescription(row("Varea", 2, cancelled)).assertExists()
        compose.onNodeWithContentDescription(row("El Cubo", 3, noCalls)).assertExists()

        // design_guidelines.md §6.3: the cancelled call is struck through and keeps its time. The decoration is read
        // from the layout, not trusted to the label.
        val layouts = mutableListOf<TextLayoutResult>()
        compose
            // The row merges its children into one utterance, so the ETA is read from the unmerged tree.
            .onNode(hasText("2 min", substring = true), useUnmergedTree = true)
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(layouts)
        assertEquals(
            TextDecoration.LineThrough,
            layouts
                .single()
                .layoutInput.style.textDecoration,
        )
    }

    @Test
    fun lb205_aStopRowOpensItsStopDetail() {
        // FR-NAV-07: the pushed stop detail from line detail. The row keeps its one utterance and gains the action,
        // so TalkBack reads the stop, its call, and "open stop" as what a double tap does.
        val stops =
            listOf(
                StopRowUi(stopId = 38, order = 1, name = "Campillo", lat = 42.45, lng = -2.44),
                StopRowUi(stopId = 39, order = 2, name = "Varea", lat = 42.46, lng = -2.43),
            )
        val opened = mutableListOf<Int>()
        compose.setContent {
            LineDetailScreen(
                LineDetailUiState.Content(line3, Direction.OUTBOUND, directions = emptyList(), stops = stops),
                onEvent = {},
                onBack = {},
                onOpenStop = { opened += it },
            )
        }

        val noCalls = context.getString(R.string.line_detail_eta_none_spoken)
        compose.onNode(hasClickAction() and hasContentDescription(row("Varea", 2, noCalls))).performClick()

        assertEquals(listOf(39), opened)
    }

    /** The row's one spoken label, as the screen builds it. */
    private fun row(
        name: String,
        order: Int,
        call: String,
    ) = context.getString(
        R.string.line_detail_stop_eta,
        context.getString(R.string.line_detail_stop_position, order, name),
        call,
    )
}
