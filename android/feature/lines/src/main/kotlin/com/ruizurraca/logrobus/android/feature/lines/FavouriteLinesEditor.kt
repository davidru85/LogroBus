package com.ruizurraca.logrobus.android.feature.lines

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.ruizurraca.logrobus.android.core.designsystem.LineBadge
import com.ruizurraca.logrobus.android.core.designsystem.ModalSheet
import com.ruizurraca.logrobus.android.core.designsystem.Tokens
import com.ruizurraca.logrobus.feature.lines.presentation.EditableFavouriteLineUi
import com.ruizurraca.logrobus.feature.lines.presentation.LinesEvent
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import com.ruizurraca.logrobus.android.core.designsystem.R as DS

/**
 * LB-110 (FR-FAV-02): the favourite lines' edit sheet, over the Lines list. A removed line goes through its toggle's
 * event, so the list's star follows; a new order goes once a drag or a move ends. Done, a swipe down or back close it.
 */
@Composable
internal fun FavouriteLinesSheet(
    rows: List<EditableFavouriteLineUi>,
    onEvent: (LinesEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalSheet(onDismissRequest = onDismiss) { close ->
        FavouriteLinesEditor(
            rows,
            onReorder = { onEvent(LinesEvent.ReorderFavourites(it)) },
            onRemove = { onEvent(LinesEvent.SetFavourite(it, isFavourite = false)) },
            onDone = close,
        )
    }
}

/**
 * The edit sheet's content: its title and Done, then a row per favourite line in the user's order. Each row has a
 * remove button, the line's badge and name, or "no longer served", and a handle to drag it by. TalkBack reads a row as
 * one, and moves it with "Move up" and "Move down" instead of a drag (NFR-A11Y).
 */
@Composable
internal fun FavouriteLinesEditor(
    rows: List<EditableFavouriteLineUi>,
    onReorder: (lineIds: List<Int>) -> Unit,
    onRemove: (lineId: Int) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The order the user sees. A drag or a move changes it here first, and the saved order then arrives in the same
    // order. A favourite removed or added, or a new saved order, replaces it, except during a drag.
    var shown by remember { mutableStateOf(rows) }
    var dragging by remember { mutableStateOf(false) }
    // Where the focus goes once a removed row has left: the next row's remove button, else the one before, else Done.
    val removeButtons = remember { mutableMapOf<Int, FocusRequester>() }
    val done = remember { FocusRequester() }
    var focusAfterRemoval by remember { mutableStateOf<FocusRequester?>(null) }
    LaunchedEffect(rows) {
        if (!dragging) shown = rows
        focusAfterRemoval?.requestFocus()
        focusAfterRemoval = null
    }
    val remove = { index: Int ->
        val next = shown.getOrNull(index + 1) ?: shown.getOrNull(index - 1)
        focusAfterRemoval = next?.let { removeButtons[it.id] } ?: done
        onRemove(shown[index].id)
    }
    val move = { from: Int, to: Int ->
        shown = shown.moved(from, to)
        onReorder(shown.map { it.id })
    }
    val list = rememberLazyListState()
    val reorderable = rememberReorderableLazyListState(list) { from, to -> shown = shown.moved(from.index, to.index) }
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(start = Tokens.Spacing.l, end = Tokens.Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.lines_favourites_edit_title),
                modifier = Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
            )
            TextButton(onClick = onDone, modifier = Modifier.focusRequester(done).focusable()) {
                Text(stringResource(R.string.lines_favourites_done))
            }
        }
        LazyColumn(Modifier.testTag(FAVOURITE_LINES_EDITOR), list) {
            // Spec §7.3: with the last favourite removed, the carousel's hint, rather than a blank sheet.
            if (shown.isEmpty()) item(key = EMPTY) { EmptyHint() }
            itemsIndexed(shown, key = { _, row -> row.id }) { index, row ->
                ReorderableItem(reorderable, key = row.id) { isDragging ->
                    // The line under the finger rises above the others.
                    Surface(
                        color = if (isDragging) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
                        shadowElevation = if (isDragging) DRAGGED_ELEVATION else 0.dp,
                    ) {
                        EditRow(
                            row,
                            position = stringResource(R.string.lines_favourites_position, index + 1, shown.size),
                            removeButton = removeButtons.getOrPut(row.id) { FocusRequester() },
                            onRemove = { remove(index) },
                            onMoveUp = if (index > 0) ({ move(index, index - 1) }) else null,
                            onMoveDown = if (index < shown.lastIndex) ({ move(index, index + 1) }) else null,
                            handle =
                                Modifier.draggableHandle(
                                    onDragStarted = { dragging = true },
                                    onDragStopped = {
                                        dragging = false
                                        onReorder(shown.map { it.id })
                                    },
                                ),
                        )
                    }
                }
            }
        }
    }
}

/** The carousel's hint, in the sheet once every favourite has been removed. TalkBack reads the star as words. */
@Composable
private fun EmptyHint() {
    val spoken = stringResource(R.string.lines_favourites_hint_spoken)
    Text(
        stringResource(R.string.lines_favourites_hint),
        modifier = Modifier.padding(Tokens.Spacing.l).semantics { contentDescription = spoken },
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Suppress("LongParameterList") // The row, its place, its remove button's focus and its three actions, its handle.
@Composable
private fun EditRow(
    row: EditableFavouriteLineUi,
    position: String,
    removeButton: FocusRequester,
    onRemove: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    handle: Modifier,
) {
    // At large text the badge goes above the name, as in the list's rows, so the name breaks between words.
    val stacked = LocalDensity.current.fontScale >= LARGE_TEXT_SCALE
    val badge = (row as? EditableFavouriteLineUi.Served)?.let { line -> @Composable { Badge(line) } }
    val actions = moveActions(onMoveUp, onMoveDown)
    ListItem(
        // The row's place is its state, so TalkBack says it on the row and again after each move (NFR-A11Y).
        modifier =
            Modifier.testTag(editRowTag(row.id)).semantics(mergeDescendants = true) {
                customActions = actions
                stateDescription = position
            },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            IconButton(onClick = onRemove, modifier = Modifier.focusRequester(removeButton).focusable()) {
                Icon(painterResource(DS.drawable.ic_remove), removeLabel(row), tint = MaterialTheme.colorScheme.error)
            }
        },
        overlineContent = badge.takeIf { stacked },
        headlineContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.m)) {
                if (!stacked) badge?.invoke()
                when (row) {
                    is EditableFavouriteLineUi.Served -> {
                        Text(row.displayName)
                    }

                    is EditableFavouriteLineUi.NoLongerServed -> {
                        Text(
                            stringResource(R.string.lines_favourites_no_longer_served),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        trailingContent = {
            // TalkBack moves the row with its actions, so the handle has nothing to say.
            Icon(
                painterResource(DS.drawable.ic_drag_handle),
                contentDescription = null,
                modifier = handle.testTag(dragHandleTag(row.id)).minimumInteractiveComponentSize(),
            )
        },
    )
}

@Composable
private fun Badge(line: EditableFavouriteLineUi.Served) = LineBadge(line.code, line.color, line.onColor)

/** The remove button's label, which names the line as the list's toggle does (FR-LINE-15). */
@Composable
private fun removeLabel(row: EditableFavouriteLineUi): String =
    when (row) {
        is EditableFavouriteLineUi.Served -> stringResource(R.string.lines_favourite_remove, row.code)
        is EditableFavouriteLineUi.NoLongerServed -> stringResource(R.string.lines_favourites_remove_no_longer_served)
    }

/** NFR-A11Y: "Move up" and "Move down", for the rows that can move that way, so TalkBack reorders without a drag. */
@Composable
private fun moveActions(
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
): List<CustomAccessibilityAction> {
    val up = stringResource(R.string.lines_favourites_move_up)
    val down = stringResource(R.string.lines_favourites_move_down)
    return listOfNotNull(
        onMoveUp?.let { move -> CustomAccessibilityAction(up) { true.also { move() } } },
        onMoveDown?.let { move -> CustomAccessibilityAction(down) { true.also { move() } } },
    )
}

private fun <T> List<T>.moved(
    from: Int,
    to: Int,
): List<T> = toMutableList().apply { add(to, removeAt(from)) }

private val DRAGGED_ELEVATION = 6.dp

private const val EMPTY = "empty"

internal const val FAVOURITE_LINES_EDITOR = "favouriteLinesEditor"

internal fun editRowTag(lineId: Int) = "editRow:$lineId"

internal fun dragHandleTag(lineId: Int) = "dragHandle:$lineId"
