package com.ruizurraca.logrobus.android.feature.lines

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.logrobus.android.core.designsystem.messageRes
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.feature.lines.domain.LinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.presentation.LinesEvent
import com.ruizurraca.logrobus.feature.lines.presentation.LinesUiMapper
import com.ruizurraca.logrobus.feature.lines.presentation.LinesUiState
import com.ruizurraca.logrobus.transit.domain.Line
import com.ruizurraca.logrobus.transit.domain.LineKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.ruizurraca.logrobus.android.core.designsystem.R as DS

/** LB-101 (FR-LINE-01, FR-LINE-02, NFR-A11Y-02) and spec §7.3 on Android: the Lines list and its states. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Tall enough for the favourites section and every row: a lazy list composes only the rows it shows.
@Config(qualifiers = "h1000dp")
class LinesScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val networkLines =
        listOf(
            line(101, "B1", "Varea - La Cava", 0xFFE30613),
            line(10, "10", "El Arco - Hospital San Pedro", 0xFFAD1457)
                .copy(headsigns = listOf("Hospital San Pedro", "El Arco")),
            line(4, "4", "Casa Blanca - Yagüe", 0xFFE30613),
            line(11, "11", "Artesanos - Manuel de Falla", 0xFF00897B, isOperational = false),
        )

    private val network = favourites()

    @Test
    fun lb101_dayLinesThenNightLines_eachUnderItsHeading_inCodeOrder() {
        compose.setContent { LinesScreen(network, onEvent = {}, onOpenMore = {}, onOpenLine = {}) }

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
        compose.setContent { LinesScreen(network, onEvent = {}, onOpenMore = {}, onOpenLine = {}) }

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
        compose.setContent { LinesScreen(network, onEvent = {}, onOpenMore = {}, onOpenLine = {}) }

        compose.onAllNodesWithContentDescription(badge("4")).fetchSemanticsNodes().let { assertEquals(1, it.size) }
        compose.onAllNodesWithContentDescription(badge("B1")).fetchSemanticsNodes().let { assertEquals(1, it.size) }
    }

    @Test
    fun lb101_eachRowIsReadAsOne_lineThenNameThenStatus() {
        compose.setContent { LinesScreen(network, onEvent = {}, onOpenMore = {}, onOpenLine = {}) }

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
        compose.setContent { LinesScreen(LinesUiState.Loading, onEvent = {}, onOpenMore = {}, onOpenLine = {}) }

        compose.onNode(isHeading() and hasText(context.getString(R.string.lines_title))).assertExists()
        compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()
    }

    @Test
    fun lb101_theFirstLoadIsAnnouncedToTalkBack() {
        // The placeholders are hidden from TalkBack, so the list says it is loading instead (design_guidelines §8).
        compose.setContent { LinesScreen(LinesUiState.Loading, onEvent = {}, onOpenMore = {}, onOpenLine = {}) }

        compose.onNode(hasContentDescription(context.getString(R.string.lines_loading))).assertExists()
    }

    @Test
    fun lb101_everyErrorReasonShowsItsMessage() {
        var reason by mutableStateOf(ErrorReason.NETWORK)
        compose.setContent { LinesScreen(LinesUiState.Error(reason), onEvent = {}, onOpenMore = {}, onOpenLine = {}) }

        ErrorReason.entries.forEach {
            reason = it
            compose.onNodeWithText(context.getString(it.messageRes)).assertExists()
        }
    }

    @Test
    fun lb101_aFirstLaunchWithNoNetworkShowsTheNetworkReason_andRetrySendsTheEvent() {
        val events = mutableListOf<LinesEvent>()
        val offline = LinesUiState.Error(ErrorReason.NETWORK)
        compose.setContent { LinesScreen(offline, onEvent = { events += it }, onOpenMore = {}, onOpenLine = {}) }

        compose.onNodeWithText(context.getString(DS.string.error_network)).assertExists()
        compose.onNodeWithText(context.getString(DS.string.error_retry)).performClick()

        assertEquals(listOf(LinesEvent.Retry), events)
    }

    @Test
    fun lb101_noLinesShowsOneSentence_andRetry() {
        val events = mutableListOf<LinesEvent>()
        compose.setContent {
            LinesScreen(LinesUiState.Empty, onEvent = { events += it }, onOpenMore = {}, onOpenLine = {})
        }

        compose.onNodeWithText(context.getString(R.string.lines_empty)).assertExists()
        compose.onNodeWithText(context.getString(DS.string.error_retry)).performClick()

        assertEquals(listOf(LinesEvent.Retry), events)
    }

    @Test
    fun lb101_theMoreActionOpensMore() {
        var opened = 0
        compose.setContent { LinesScreen(network, onEvent = {}, onOpenMore = { opened++ }, onOpenLine = {}) }

        compose.onNode(hasContentDescription(context.getString(MORE_ACTION))).performClick()

        assertEquals(1, opened)
    }

    @Test
    fun lb103_withNoFavouritesTheSectionShowsOneHint() {
        // FR-LINE-14: the section never disappears, so the feature stays discoverable.
        compose.setContent { LinesScreen(network, onEvent = {}, onOpenMore = {}, onOpenLine = {}) }

        compose.onNode(isHeading() and hasText(context.getString(R.string.lines_favourites_title))).assertExists()
        compose.onAllNodesWithText(context.getString(R.string.lines_favourites_hint)).assertCountEquals(1)
    }

    @Test
    fun lb103_eachRowHasAToggleLabelledByItsLine_thatSendsItsNewValue() {
        // FR-LINE-15: "Add line 4 to favourites", then "Remove line 10 from favourites" for a favourite.
        val events = mutableListOf<LinesEvent>()
        compose.setContent {
            LinesScreen(favourites(10), onEvent = { events += it }, onOpenMore = {}, onOpenLine = {})
        }

        compose.onNodeWithContentDescription(context.getString(R.string.lines_favourite_add, "4")).performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.lines_favourite_remove, "10")).performClick()

        assertEquals(
            listOf(LinesEvent.SetFavourite(lineId = 4, isFavourite = true), LinesEvent.SetFavourite(10, false)),
            events,
        )
    }

    @Test
    fun lb103_aFavouriteCardShowsItsBadgeNameAndHeadsigns_andOpensItsLine() {
        // FR-LINE-13. TalkBack reads the headsigns as words, never as arrows.
        val opened = mutableListOf<Int>()
        compose.setContent {
            LinesScreen(favourites(10), onEvent = {}, onOpenMore = {}, onOpenLine = { opened += it })
        }

        val card = compose.onNode(hasContentDescription(towards("Hospital San Pedro", "El Arco")) and hasClickAction())
        card
            .assert(hasContentDescription(badge("10")))
            .assert(hasText("El Arco - Hospital San Pedro"))
            // No-break spaces keep each arrow with its headsign when the text wraps.
            .assert(hasText("→\u00A0Hospital San Pedro\u00A0· →\u00A0El Arco"))
        card.performClick()

        assertEquals(listOf(10), opened)
    }

    @Test
    fun lb103_rowsAndCardsSayTheyOpenTheLine() {
        // TalkBack: "double-tap to open line", rather than "double-tap to activate".
        compose.setContent { LinesScreen(favourites(10), onEvent = {}, onOpenMore = {}, onOpenLine = {}) }

        val open = context.getString(R.string.lines_open_line)
        listOf(
            compose.onNode(hasText("Casa Blanca - Yagüe") and hasClickAction()),
            compose.onNode(hasContentDescription(towards("Hospital San Pedro", "El Arco")) and hasClickAction()),
        ).forEach { node ->
            assertEquals(open, node.fetchSemanticsNode().config[SemanticsActions.OnClick].label)
        }
    }

    @Test
    fun lb103_theHintIsReadAsWords_notAsAStarSymbol() {
        compose.setContent { LinesScreen(network, onEvent = {}, onOpenMore = {}, onOpenLine = {}) }

        compose
            .onNodeWithText(context.getString(R.string.lines_favourites_hint))
            .assert(hasContentDescription(context.getString(R.string.lines_favourites_hint_spoken)))
    }

    @Test
    fun lb103_atLargeTextTheBadgeSitsAboveTheName_soNamesBreakBetweenWords() {
        // At 200 %, the toggle beside a name would leave it too narrow for "Valdegastea".
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                LinesScreen(network, onEvent = {}, onOpenMore = {}, onOpenLine = {})
            }
        }

        val badge = compose.onNodeWithContentDescription(badge("4"), useUnmergedTree = true).fetchSemanticsNode()
        val name = compose.onNodeWithText("Casa Blanca - Yagüe", useUnmergedTree = true).fetchSemanticsNode()
        assertTrue("the badge ends above the name", badge.boundsInRoot.bottom <= name.boundsInRoot.top)
    }

    @Test
    fun lb103_tappingARowOpensItsLine() {
        // So that a line can be added to favourites from its detail too (FR-LINE-09).
        val opened = mutableListOf<Int>()
        compose.setContent { LinesScreen(network, onEvent = {}, onOpenMore = {}, onOpenLine = { opened += it }) }

        compose.onNode(hasText("Casa Blanca - Yagüe") and hasClickAction()).performClick()

        assertEquals(listOf(4), opened)
    }

    private fun favourites(vararg ids: Int) =
        LinesUiMapper.state(AppResult.Success(LinesWithFavourites(networkLines, favouriteIds = ids.toList())))

    private fun towards(vararg headsigns: String) =
        headsigns.joinToString(", ") { context.getString(R.string.lines_towards, it) }

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
