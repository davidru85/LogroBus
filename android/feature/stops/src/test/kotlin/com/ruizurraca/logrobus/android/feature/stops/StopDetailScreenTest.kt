package com.ruizurraca.logrobus.android.feature.stops

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextDecoration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.logrobus.core.presentation.EtaDisplay
import com.ruizurraca.logrobus.feature.stops.presentation.ServedArrivalUi
import com.ruizurraca.logrobus.feature.stops.presentation.StopDetailEmptyUi
import com.ruizurraca.logrobus.feature.stops.presentation.StopDetailUiState
import com.ruizurraca.logrobus.feature.stops.presentation.StopUpdatedUi
import com.ruizurraca.logrobus.transit.domain.Direction
import com.ruizurraca.logrobus.transit.domain.ServedLine
import com.ruizurraca.logrobus.transit.domain.Stop
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import com.ruizurraca.logrobus.android.core.designsystem.R as DS

/**
 * LB-205 (FR-STOP-03 to FR-STOP-05, spec §5.7): the stop detail's header, its rows and the age of its list. The ETAs
 * are the design system's, so the test checks what the screen hands them and the label each row is spoken as.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StopDetailScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val stop =
        Stop(
            id = 38,
            name = "Gonzalo de Berceo",
            lat = 42.4650,
            lng = -2.4456,
            isOperational = true,
            isProvisional = false,
            servedLines =
                listOf(
                    ServedLine(1, "1", "Hospital San Pedro", Direction.OUTBOUND, 0xFFE30613L, 0xFFFFFFFFL),
                    ServedLine(33, "33", "El Arco", Direction.INBOUND, 0xFF1E7A3FL, 0xFFFFFFFFL),
                ),
        )

    @Test
    fun lb205_eachArrivalIsOneUtterance_andACancelledOneIsStruckThrough() {
        // FR-STOP-04, design_guidelines.md §6.3: the row reads "Line 1, Towards Hospital San Pedro, in 4
        // minutes", and a cancelled call keeps its time, struck through with its "Cancelled" label.
        val rows =
            listOf(
                ServedArrivalUi(
                    "1",
                    0xFFE30613L,
                    0xFFFFFFFFL,
                    "Hospital San Pedro",
                    EtaDisplay.Minutes(4),
                    false,
                    false,
                ),
                ServedArrivalUi("33", 0xFF1E7A3FL, 0xFFFFFFFFL, "El Arco", EtaDisplay.Minutes(2), true, false),
            )
        compose.setContent {
            StopDetailScreen(
                StopDetailUiState.Content(stop, rows, StopUpdatedUi(12, isStale = false, needsRefresh = false)),
                onRetry = {},
                onBack = {},
            )
        }

        val fourMinutes =
            context.resources.getQuantityString(DS.plurals.eta_minutes_spoken, 4, 4)
        val cancelled = context.getString(DS.string.eta_cancelled_spoken)
        compose
            .onNodeWithContentDescription(row(badge("1"), "Hospital San Pedro", fourMinutes))
            .assertExists()
        compose.onNodeWithContentDescription(row(badge("33"), "El Arco", cancelled)).assertExists()

        // The strike-through is read from the layout, not trusted to the label.
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
    fun lb206_anEmptyListSaysThereAreNoBuses() {
        compose.setContent {
            StopDetailScreen(
                StopDetailUiState.Content(
                    stop,
                    emptyList(),
                    StopUpdatedUi(12, isStale = false, needsRefresh = false),
                    empty = StopDetailEmptyUi.NO_BUSES,
                ),
                onRetry = {},
                onBack = {},
            )
        }

        // The screen words its strings from the resources of the locale it runs in, which in these tests is English.
        compose.onNodeWithText("No buses in the next 60 minutes").assertExists()
    }

    @Test
    fun lb206_aNightOnlyStopAddsTheSentenceFromRemoteConfig() {
        // FR-STOP-06: the night sentence is config's, so the test passes one and reads it back in the message.
        compose.setContent {
            StopDetailScreen(
                StopDetailUiState.Content(
                    stop,
                    emptyList(),
                    StopUpdatedUi(12, isStale = false, needsRefresh = false),
                    empty = StopDetailEmptyUi.NO_BUSES_THEN_NIGHT,
                ),
                onRetry = {},
                onBack = {},
                nightSentence = "Night service, weekends.",
            )
        }

        // The message's own words come from the resources; the night sentence is the one config supplied.
        compose
            .onNodeWithText("No buses in the next 60 minutes. Night service, weekends.")
            .assertExists()
    }

    @Test
    fun lb205_theAgeShowsInTheWarningColour_andPast120SecondsItOffersTapToRefresh() {
        // FR-STOP-05, design_guidelines.md §7: 50 s words "Updated 50 s ago" in the warning colour; past 120 s the
        // ETAs are clock times and the screen offers the refresh action, which sends Retry.
        val rows =
            listOf(
                ServedArrivalUi(
                    "1",
                    0xFFE30613L,
                    0xFFFFFFFFL,
                    "Hospital San Pedro",
                    EtaDisplay.ClockTime(18, 42),
                    isCancelled = false,
                    isLowConfidence = false,
                ),
            )
        var retries = 0
        compose.setContent {
            StopDetailScreen(
                StopDetailUiState.Content(stop, rows, StopUpdatedUi(121, isStale = true, needsRefresh = true)),
                onRetry = { retries++ },
                onBack = {},
            )
        }

        val warning = mutableListOf<TextLayoutResult>()
        compose
            .onNode(hasText(context.getString(R.string.stop_detail_updated_ago, 121)))
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(warning)
        // design_guidelines.md §6.1: the placeholder warning colour, which StatusColors carries.
        assertEquals(
            Color(0xFF8A5300),
            warning
                .single()
                .layoutInput.style.color,
        )

        compose.onNodeWithText(context.getString(R.string.stop_detail_tap_to_refresh)).performClick()
        assertEquals(1, retries)
    }

    /** The badge's own spoken label, which the row reads first. */
    private fun badge(code: String) = context.getString(DS.string.line_badge, code)

    /** The row's one spoken label, as the screen builds it. */
    private fun row(
        badge: String,
        headsign: String,
        call: String,
    ) = context.getString(
        R.string.stop_detail_arrival_headsign_spoken,
        badge,
        context.getString(R.string.stop_detail_towards, headsign),
        call,
    )
}
