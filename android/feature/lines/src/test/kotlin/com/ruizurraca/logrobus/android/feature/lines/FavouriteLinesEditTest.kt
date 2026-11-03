package com.ruizurraca.logrobus.android.feature.lines

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.feature.lines.domain.LinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailEvent
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailUi
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailUiState
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

/**
 * LB-110 (FR-FAV-02, FR-FAV-05, NFR-A11Y) on Android: the carousel's Edit opens a sheet with every favourite line in
 * the user's order, where a line is dragged by its handle, moved with TalkBack's actions, or removed, including one
 * that is no longer served. A star tapped at the limit shows why nothing was saved.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "h1000dp")
class FavouriteLinesEditTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val edit get() = context.getString(R.string.lines_favourites_edit_title)
    private val done get() = context.getString(R.string.lines_favourites_done)
    private val moveUp get() = context.getString(R.string.lines_favourites_move_up)
    private val moveDown get() = context.getString(R.string.lines_favourites_move_down)

    private val networkLines =
        listOf(
            line(101, "B1", "Varea - La Cava"),
            line(10, "10", "El Arco - Hospital San Pedro"),
            line(4, "4", "Casa Blanca - Yagüe"),
        )

    private val events = mutableListOf<LinesEvent>()

    @Test
    fun lb110_editShowsOnlyWithFavouritesToEdit() {
        var state by mutableStateOf(favourites())
        compose.setContent { LinesScreen(state, onEvent = {}, onOpenMore = {}, onOpenLine = {}) }
        compose.onAllNodesWithContentDescription(edit).assertCountEquals(0)

        state = favourites(10)

        // TalkBack names it by what it edits; its name keeps the word it shows (WCAG 2.5.3).
        compose.onNodeWithContentDescription(edit).assertExists()
        compose.onNodeWithText(context.getString(R.string.lines_favourites_edit)).assertExists()
    }

    @Test
    fun lb110_aLineNoLongerServedAloneStillOffersEdit() {
        // It has no card, so the carousel shows its hint, but it counts towards the limit until it is removed.
        compose.setContent { LinesScreen(favourites(99), onEvent = {}, onOpenMore = {}, onOpenLine = {}) }

        compose.onNodeWithContentDescription(edit).assertExists()
    }

    @Test
    fun lb110_editOpensASheetWithEveryFavouriteInTheUsersOrder_andDoneClosesIt() {
        open(favourites(10, 99, 4))

        compose.onNode(isHeading() and hasText(edit)).assertExists()
        assertEquals(listOf(10, 99, 4), rowsInTheSheet())
        compose
            .onNode(
                inTheSheet and hasText(context.getString(R.string.lines_favourites_no_longer_served)),
            ).assertExists()

        compose.onNodeWithText(done).performClick()

        compose.onAllNodes(isHeading() and hasText(edit)).assertCountEquals(0)
    }

    @Test
    fun lb110_removingALineSendsItsToggleOff() {
        open(favourites(10, 4))

        compose.onNode(inTheSheet and hasContentDescription(remove("10"))).performClick()

        assertEquals(listOf<LinesEvent>(LinesEvent.SetFavourite(10, isFavourite = false)), events)
    }

    @Test
    fun lb110_aLineNoLongerServedCanBeRemoved() {
        open(favourites(10, 99))

        compose
            .onNode(
                inTheSheet and
                    hasContentDescription(context.getString(R.string.lines_favourites_remove_no_longer_served)),
            ).performClick()

        assertEquals(listOf<LinesEvent>(LinesEvent.SetFavourite(99, isFavourite = false)), events)
    }

    @Test
    fun lb110_talkBackMovesALineUpOrDown_withoutDragging() {
        open(favourites(10, 4, 101))

        // The first line cannot go up, nor the last one down.
        assertEquals(listOf(moveDown), actionsOf(row(10)))
        assertEquals(listOf(moveUp, moveDown), actionsOf(row(4)))
        assertEquals(listOf(moveUp), actionsOf(row(101)))

        // Each row says where it is, so a move is heard (NFR-A11Y).
        assertEquals(position(2, 3), stateOf(row(4)))
        perform(moveUp, row(4))

        assertEquals(listOf<LinesEvent>(LinesEvent.ReorderFavourites(listOf(4, 10, 101))), events)
        assertEquals(listOf(4, 10, 101), rowsInTheSheet())
        assertEquals(position(1, 3), stateOf(row(4)))
    }

    @Test
    fun lb110_draggingTheThirdLineToTheTop_savesTheNewOrderOnceTheDragEnds() {
        open(favourites(10, 4, 101))
        val rowHeight = row(10).fetchSemanticsNode().boundsInRoot.height

        // The handle says nothing to TalkBack, so it is part of its row in the merged tree.
        val handle = compose.onNodeWithTag(dragHandleTag(101), useUnmergedTree = true)
        handle.performTouchInput { down(center) }
        // Step by step, with a frame between steps, as a finger moves: the list makes room as the line passes.
        repeat(DRAG_STEPS) {
            handle.performTouchInput { moveBy(Offset(0f, -rowHeight * 2.5f / DRAG_STEPS)) }
            compose.mainClock.advanceTimeByFrame()
        }
        assertEquals("nothing is saved during the drag", emptyList<LinesEvent>(), events)
        handle.performTouchInput { up() }
        compose.waitForIdle()

        assertEquals(listOf<LinesEvent>(LinesEvent.ReorderFavourites(listOf(101, 10, 4))), events)
        assertEquals(listOf(101, 10, 4), rowsInTheSheet())
    }

    @Test
    fun lb110_theSheetFollowsAFavouriteRemovedOrAdded() {
        var state by mutableStateOf(favourites(10, 4))
        compose.setContent { LinesScreen(state, onEvent = { events += it }, onOpenMore = {}, onOpenLine = {}) }
        compose.onNodeWithContentDescription(edit).performClick()

        state = favourites(4, 101)

        assertEquals(listOf(4, 101), rowsInTheSheet())
    }

    @Test
    fun lb110_talkBackReadsARowAsItsLineAndName_withoutTheHandle() {
        open(favourites(10, 4))

        val row = row(10).fetchSemanticsNode().config
        assertEquals(listOf(badge("10")), row.getOrNull(SemanticsProperties.ContentDescription))
        assertEquals(listOf("El Arco - Hospital San Pedro"), row.getOrNull(SemanticsProperties.Text)?.map { it.text })
    }

    @Test
    fun lb110_removingALineMovesTheFocusToTheNextLinesRemoveButton() {
        // The removed row takes its button, and TalkBack's focus, with it (design_guidelines.md §8).
        openFollowing(favourites(10, 4, 101))

        compose.onNode(inTheSheet and hasContentDescription(remove("10"))).performClick()

        compose.onNode(inTheSheet and hasContentDescription(remove("4"))).assertIsFocused()
    }

    @Test
    fun lb110_removingTheLastLineMovesTheFocusToTheLineBefore() {
        openFollowing(favourites(10, 4))

        compose.onNode(inTheSheet and hasContentDescription(remove("4"))).performClick()

        compose.onNode(inTheSheet and hasContentDescription(remove("10"))).assertIsFocused()
    }

    @Test
    fun lb110_removingTheOnlyLineShowsTheHint_andMovesTheFocusToDone() {
        // Spec §7.3: the sheet never goes blank.
        openFollowing(favourites(10))

        compose.onNode(inTheSheet and hasContentDescription(remove("10"))).performClick()

        compose.onNode(hasText(context.getString(R.string.lines_favourites_hint))).assertExists()
        compose.onNodeWithText(done).assertIsFocused()
    }

    @Test
    fun lb110_doneGivesTheFocusBackToEdit() {
        open(favourites(10, 4))

        compose.onNodeWithText(done).performClick()

        compose.onNodeWithContentDescription(edit).assertIsFocused()
    }

    @Test
    fun lb110_aChangeFromTheDatabaseDuringADragWaitsForTheDragToEnd() {
        var state by mutableStateOf(favourites(10, 4, 101))
        compose.setContent { LinesScreen(state, onEvent = { events += it }, onOpenMore = {}, onOpenLine = {}) }
        compose.onNodeWithContentDescription(edit).performClick()
        val rowHeight = row(10).fetchSemanticsNode().boundsInRoot.height
        val handle = compose.onNodeWithTag(dragHandleTag(101), useUnmergedTree = true)
        handle.performTouchInput { down(center) }
        repeat(DRAG_STEPS) {
            handle.performTouchInput { moveBy(Offset(0f, -rowHeight * 2.5f / DRAG_STEPS)) }
            compose.mainClock.advanceTimeByFrame()
        }

        // Another screen saves line 4 first while the finger is down: the dragged order holds.
        state = favourites(4, 10, 101)
        compose.waitForIdle()
        assertEquals(listOf(101, 10, 4), rowsInTheSheet())

        handle.performTouchInput { up() }
        compose.waitForIdle()
        assertEquals(listOf<LinesEvent>(LinesEvent.ReorderFavourites(listOf(101, 10, 4))), events)
    }

    @Test
    fun lb110_theFavouritesHeadingIsAsTallWithEditAsWithout() {
        // Starring the first favourite, or a first load with favourites, must not move the list (spec §7.3).
        var state by mutableStateOf(favourites())
        compose.setContent { LinesScreen(state, onEvent = {}, onOpenMore = {}, onOpenLine = {}) }
        val hint = context.getString(R.string.lines_favourites_hint)
        val without =
            compose
                .onNode(hasText(hint))
                .fetchSemanticsNode()
                .boundsInRoot.top

        state = favourites(99)

        assertEquals(
            without,
            compose
                .onNode(hasText(hint))
                .fetchSemanticsNode()
                .boundsInRoot.top,
            0.5f,
        )
    }

    @Test
    fun lb110_theLimitMessageCountsAsShown_evenWhenTheScreenLeavesFirst() {
        // Opening a line within the message's 10 s must not bring it back on return.
        var onScreen by mutableStateOf(true)
        compose.setContent {
            if (onScreen) {
                LinesScreen(
                    favourites(),
                    onEvent = { events += it },
                    onOpenMore = {},
                    onOpenLine = {},
                    favouriteLimit = 20,
                )
            }
        }
        compose.onNodeWithText(limitMessage(20)).assertExists()

        onScreen = false
        compose.waitForIdle()

        assertEquals(listOf<LinesEvent>(LinesEvent.FavouriteLimitShown), events)
    }

    @Test
    fun lb110_aStarBeyondTheLimitSaysWhy_thenTellsTheViewModelItWasShown() {
        compose.setContent {
            LinesScreen(favourites(), onEvent = { events += it }, onOpenMore = {}, onOpenLine = {}, favouriteLimit = 20)
        }

        compose.onNodeWithText(limitMessage(20)).assertExists()
        assertTrue(LinesEvent.FavouriteLimitShown !in events)

        compose.mainClock.advanceTimeBy(MESSAGE_SHOWN_MILLIS)

        compose.onAllNodesWithText(limitMessage(20)).assertCountEquals(0)
        assertEquals(listOf<LinesEvent>(LinesEvent.FavouriteLimitShown), events)
    }

    @Test
    fun lb110_theLineDetailsStarBeyondTheLimitSaysWhyToo() {
        val detail = mutableListOf<LineDetailEvent>()
        val line = LineDetailUi(4, "4", "Casa Blanca - Yagüe", 0xFFE30613, 0xFFFFFFFF, isFavourite = false)
        compose.setContent {
            LineDetailScreen(LineDetailUiState.Content(line), { detail += it }, onBack = {}, favouriteLimit = 20)
        }

        compose.onNodeWithText(limitMessage(20)).assertExists()

        compose.mainClock.advanceTimeBy(MESSAGE_SHOWN_MILLIS)

        assertEquals(listOf<LineDetailEvent>(LineDetailEvent.FavouriteLimitShown), detail)
    }

    private fun open(state: LinesUiState) {
        compose.setContent { LinesScreen(state, onEvent = { events += it }, onOpenMore = {}, onOpenLine = {}) }
        compose.onNodeWithContentDescription(edit).performClick()
    }

    /** Opens the sheet over a state that follows the removals, as the database's flow does. */
    private fun openFollowing(initial: LinesUiState) {
        var state by mutableStateOf(initial)
        val ids = (initial as LinesUiState.Content).editable.map { it.id }.toMutableList()
        compose.setContent {
            LinesScreen(
                state,
                onEvent = { event ->
                    events += event
                    if (event is LinesEvent.SetFavourite && !event.isFavourite) {
                        ids -= event.lineId
                        state = favourites(*ids.toIntArray())
                    }
                },
                onOpenMore = {},
                onOpenLine = {},
            )
        }
        compose.onNodeWithContentDescription(edit).performClick()
    }

    private fun stateOf(row: SemanticsNodeInteraction) =
        row.fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)

    private fun position(
        at: Int,
        of: Int,
    ) = context.getString(R.string.lines_favourites_position, at, of)

    private fun badge(code: String) = context.getString(DS.string.line_badge, code)

    private val inTheSheet = hasAnyAncestor(hasTestTag(FAVOURITE_LINES_EDITOR))

    private fun row(id: Int) = compose.onNode(hasTestTag(editRowTag(id)))

    /** The ids of the sheet's rows, top to bottom. */
    private fun rowsInTheSheet(): List<Int> =
        compose
            .onAllNodes(
                SemanticsMatcher(
                    "an edit row",
                ) {
                    it.config
                        .getOrNull(SemanticsProperties.TestTag)
                        .orEmpty()
                        .startsWith(ROW)
                },
            ).fetchSemanticsNodes()
            .sortedBy { it.boundsInRoot.top }
            .map { it.config[SemanticsProperties.TestTag].removePrefix(ROW).toInt() }

    private fun actionsOf(row: SemanticsNodeInteraction): List<String> =
        row
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.CustomActions)
            .orEmpty()
            .map { it.label }

    private fun perform(
        label: String,
        row: SemanticsNodeInteraction,
    ) {
        val action = row.fetchSemanticsNode().config[SemanticsActions.CustomActions].first { it.label == label }
        compose.runOnIdle { action.action() }
        compose.waitForIdle()
    }

    private fun remove(code: String) = context.getString(R.string.lines_favourite_remove, code)

    private fun limitMessage(limit: Int) =
        context.resources.getQuantityString(R.plurals.lines_favourites_limit, limit, limit)

    private fun favourites(vararg ids: Int) =
        LinesUiMapper.state(AppResult.Success(LinesWithFavourites(networkLines, favouriteIds = ids.toList())))

    private fun line(
        id: Int,
        code: String,
        name: String,
    ) = Line(
        id,
        code,
        name,
        0xFFE30613,
        0xFFFFFFFF,
        if (code.startsWith("B")) LineKind.NIGHT else LineKind.DAY,
        isOperational = true,
    )

    private companion object {
        const val ROW = "editRow:"
        const val DRAG_STEPS = 20

        // A long snackbar shows for 10 s; a little more lets it leave.
        const val MESSAGE_SHOWN_MILLIS = 12_000L
    }
}
