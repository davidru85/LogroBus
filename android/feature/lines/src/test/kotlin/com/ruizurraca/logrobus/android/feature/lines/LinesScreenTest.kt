package com.ruizurraca.logrobus.android.feature.lines

import android.content.Context
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.feature.lines.presentation.LinesEvent
import com.ruizurraca.logrobus.feature.lines.presentation.LinesUiMapper
import com.ruizurraca.logrobus.feature.lines.presentation.LinesUiState
import com.ruizurraca.logrobus.transit.domain.Line
import com.ruizurraca.logrobus.transit.domain.LineKind
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import com.ruizurraca.logrobus.android.core.designsystem.R as DS

/** LB-101 (FR-LINE-01, FR-LINE-02, NFR-A11Y-02) and spec §7.3 on Android: the Lines list and its states. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LinesScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val network =
        LinesUiMapper.state(
            AppResult.Success(
                listOf(
                    line(101, "B1", "Varea - La Cava", 0xFFE30613),
                    line(10, "10", "El Arco - Hospital San Pedro", 0xFFAD1457),
                    line(4, "4", "Casa Blanca - Yagüe", 0xFFE30613),
                    line(11, "11", "Artesanos - Manuel de Falla", 0xFF00897B, isOperational = false),
                ),
            ),
        )

    @Test
    fun lb101_dayLinesThenNightLines_eachUnderItsHeading_inCodeOrder() {
        compose.setContent { LinesScreen(network, onEvent = {}, onOpenMore = {}) }

        val day = context.getString(R.string.lines_group_day)
        val night = context.getString(R.string.lines_group_night)
        val expected = listOf(day, badge("4"), badge("10"), badge("11"), night, badge("B1"))
        // The group headings and the badges, in the order the list lays them out.
        val labels =
            compose
                .onAllNodes(isHeading() or SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription), true)
                .fetchSemanticsNodes()
                .map { node ->
                    node.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
                        ?: node.config
                            .getOrNull(SemanticsProperties.Text)
                            ?.firstOrNull()
                            ?.text
                }.filter { it in expected }
        assertEquals(expected, labels)
    }

    @Test
    fun lb101_aLineThatIsNotOperationalShowsAStatusChip() {
        compose.setContent { LinesScreen(network, onEvent = {}, onOpenMore = {}) }

        compose
            .onNode(hasText(context.getString(R.string.lines_not_operational)))
            .assertExists()
        compose
            .onAllNodes(hasText(context.getString(R.string.lines_not_operational)))
            .fetchSemanticsNodes()
            .let { assertEquals(1, it.size) }
    }

    @Test
    fun lb101_linesThatShareAColourAreSpokenByTheirCode_neverTheColour() {
        // DD-08: lines 4 and B1 share red.
        compose.setContent { LinesScreen(network, onEvent = {}, onOpenMore = {}) }

        compose.onAllNodesWithContentDescription(badge("4")).fetchSemanticsNodes().let { assertEquals(1, it.size) }
        compose.onAllNodesWithContentDescription(badge("B1")).fetchSemanticsNodes().let { assertEquals(1, it.size) }
    }

    @Test
    fun lb101_eachRowIsReadAsOne_lineThenNameThenStatus() {
        compose.setContent { LinesScreen(network, onEvent = {}, onOpenMore = {}) }

        val row = compose.onNode(hasText("Artesanos - Manuel de Falla", substring = true)).fetchSemanticsNode()
        val spoken =
            row.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() +
                row.config
                    .getOrNull(SemanticsProperties.Text)
                    .orEmpty()
                    .map { it.text }
        assertEquals(
            listOf(badge("11"), "Artesanos - Manuel de Falla", context.getString(R.string.lines_not_operational)),
            spoken,
        )
    }

    @Test
    fun lb101_theFirstLoadShowsPlaceholders_notASpinner() {
        compose.setContent { LinesScreen(LinesUiState.Loading, onEvent = {}, onOpenMore = {}) }

        compose.onNode(isHeading() and hasText(context.getString(R.string.lines_title))).assertExists()
        compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()
    }

    @Test
    fun lb101_aFirstLaunchWithNoNetworkShowsTheNetworkReason_andRetrySendsTheEvent() {
        val events = mutableListOf<LinesEvent>()
        compose.setContent {
            LinesScreen(LinesUiState.Error(ErrorReason.NETWORK), onEvent = { events += it }, onOpenMore = {})
        }

        compose.onNodeWithText(context.getString(DS.string.error_network)).assertExists()
        compose.onNodeWithText(context.getString(DS.string.error_retry)).performClick()

        assertEquals(listOf(LinesEvent.Retry), events)
    }

    @Test
    fun lb101_noLinesShowsOneSentence_andRetry() {
        val events = mutableListOf<LinesEvent>()
        compose.setContent { LinesScreen(LinesUiState.Empty, onEvent = { events += it }, onOpenMore = {}) }

        compose.onNodeWithText(context.getString(R.string.lines_empty)).assertExists()
        compose.onNodeWithText(context.getString(DS.string.error_retry)).performClick()

        assertEquals(listOf(LinesEvent.Retry), events)
    }

    @Test
    fun lb101_theMoreActionOpensMore() {
        var opened = 0
        compose.setContent { LinesScreen(network, onEvent = {}, onOpenMore = { opened++ }) }

        compose.onNode(hasContentDescription(context.getString(MORE_ACTION))).performClick()

        assertEquals(1, opened)
    }

    private fun badge(code: String) = context.getString(BADGE_LABEL, code)

    private fun line(
        id: Int,
        code: String,
        name: String,
        color: Long,
        isOperational: Boolean = true,
    ) = Line(
        id,
        code,
        name,
        color,
        0xFFFFFFFF,
        if (code.startsWith("B")) LineKind.NIGHT else LineKind.DAY,
        isOperational,
    )

    private companion object {
        val BADGE_LABEL = DS.string.line_badge
        val MORE_ACTION = DS.string.action_more
    }
}
