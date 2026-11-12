package com.ruizurraca.logrobus.android.feature.nearme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.ruizurraca.logrobus.android.core.designsystem.Tokens
import com.ruizurraca.logrobus.favourites.domain.SavedStopAlias
import com.ruizurraca.logrobus.feature.nearme.presentation.SavedStopRowUi
import com.ruizurraca.logrobus.android.core.designsystem.R as DS

/** The minimum touch target a row's handle takes (NFR-A11Y), as the favourite lines' editor gives it. */
private val HANDLE_TOUCH_TARGET = 48.dp

/**
 * LB-208 part 3 (FR-FAV-02): "My stops"' edit sheet. Each row can be moved, renamed or removed, and the order is the
 * one the section shows.
 *
 * Moving is a **custom accessibility action** rather than a drag, which TalkBack cannot perform, and rather than two
 * icon buttons: the favourite lines' editor made the same choice, so both screens read and work the same way. The row's
 * place is its state, so TalkBack says it on the row and again after each move.
 */
@Composable
fun MyStopsEditSheet(
    rows: List<SavedStopRowUi>,
    onReorder: (List<Int>) -> Unit,
    onRename: (stopId: Int, alias: String?) -> Unit,
    onDelete: (stopId: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var renaming by remember { mutableStateOf<SavedStopRowUi?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.nearme_my_stops_edit_title),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = {
            LazyColumn {
                itemsIndexed(rows, key = { _, row -> row.stopId }) { index, row ->
                    EditableRow(
                        row = row,
                        place = RowPlace(index, rows.size),
                        actions =
                            RowActions(
                                moveUp = { onReorder(rows.moved(index, -1)) },
                                moveDown = { onReorder(rows.moved(index, 1)) },
                                rename = { renaming = row },
                                delete = { onDelete(row.stopId) },
                            ),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.nearme_my_stops_done)) }
        },
    )
    renaming?.let { row ->
        RenameDialog(
            row = row,
            onDismiss = { renaming = null },
            onConfirm = { alias ->
                renaming = null
                onRename(row.stopId, alias)
            },
        )
    }
}

/** One editable row: its place as its state, its two move actions, and its remove and rename buttons. */
@Composable
private fun EditableRow(
    row: SavedStopRowUi,
    place: RowPlace,
    actions: RowActions,
) {
    val spokenPlace = stringResource(R.string.nearme_my_stops_position, place.index + 1, place.count)
    val up = stringResource(R.string.nearme_my_stops_move_up)
    val down = stringResource(R.string.nearme_my_stops_move_down)
    val custom =
        buildList {
            if (place.index > 0) add(CustomAccessibilityAction(up) { true.also { actions.moveUp() } })
            if (place.index < place.count - 1) add(CustomAccessibilityAction(down) { true.also { actions.moveDown() } })
        }
    ListItem(
        // The row's place is its state, so TalkBack says it on the row and again after each move (NFR-A11Y-02).
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    customActions = custom
                    stateDescription = spokenPlace
                },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(row.label, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = { Text(spokenPlace, style = MaterialTheme.typography.labelMedium) },
        leadingContent = {
            IconButton(onClick = actions.delete) {
                Icon(
                    painterResource(DS.drawable.ic_remove),
                    contentDescription = stringResource(R.string.nearme_my_stops_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.s)) {
                TextButton(onClick = actions.rename) { Text(stringResource(R.string.nearme_my_stops_rename)) }
                // TalkBack moves the row with its actions, so the handle has nothing to say.
                Icon(
                    painterResource(DS.drawable.ic_drag_handle),
                    contentDescription = null,
                    modifier = Modifier.size(HANDLE_TOUCH_TARGET),
                )
            }
        },
    )
}

/** FR-STOP-08: a row's new alias, cut at thirty characters as it is typed, as the save action's field is. */
@Composable
private fun RenameDialog(
    row: SavedStopRowUi,
    onDismiss: () -> Unit,
    onConfirm: (alias: String?) -> Unit,
) {
    var text by remember { mutableStateOf(row.label) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.nearme_my_stops_rename)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { typed ->
                    text =
                        if (typed.length <= SavedStopAlias.MAX_LENGTH) {
                            typed
                        } else {
                            SavedStopAlias.of(typed).orEmpty()
                        }
                },
                label = { Text(row.label) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(SavedStopAlias.of(text)) }) {
                Text(stringResource(R.string.nearme_my_stops_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.nearme_my_stops_cancel)) }
        },
    )
}

/** A row's place in the sheet: its index and how many rows there are. */
private data class RowPlace(
    val index: Int,
    val count: Int,
)

/** What an editable row can do: move itself, be renamed, or be removed. */
private class RowActions(
    val moveUp: () -> Unit,
    val moveDown: () -> Unit,
    val rename: () -> Unit,
    val delete: () -> Unit,
)

/**
 * The rows with the item at [index] moved by [offset], never past either end. An empty list, or a move that would leave
 * the list, answers the order it already has.
 */
internal fun List<SavedStopRowUi>.moved(
    index: Int,
    offset: Int,
): List<Int> {
    // An empty list has no valid range to coerce into, so it answers before the coerce runs.
    if (isEmpty()) return emptyList()
    val target = (index + offset).coerceIn(0, lastIndex)
    return if (target == index) {
        map { it.stopId }
    } else {
        toMutableList().apply { add(target, removeAt(index)) }.map { it.stopId }
    }
}
