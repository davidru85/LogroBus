package com.ruizurraca.logrobus.android.feature.stops

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.ruizurraca.logrobus.android.core.designsystem.FavouriteToggle
import com.ruizurraca.logrobus.favourites.domain.SavedStopAlias

/** The longest alias the field takes, re-exported for the dialog's tests and the screen's reader. */
internal val ALIAS_MAX_LENGTH: Int get() = SavedStopAlias.MAX_LENGTH

/**
 * FR-STOP-08: the stop detail's save action. A saved stop shows the design system's filled star, labelled with its
 * alias, and tapping it removes the stop; an unsaved one shows the empty star, and tapping it asks for an alias before
 * saving. It is [FavouriteToggle] rather than a new control, so the stop reads the same as a favourite line.
 */
@Composable
fun SaveAction(
    save: SavedStopUi,
    onSave: (alias: String?) -> Unit,
) {
    var asking by remember { mutableStateOf(false) }
    val label =
        if (save.isSaved) {
            stringResource(R.string.stop_detail_unsave, save.alias.orEmpty())
        } else {
            stringResource(R.string.stop_detail_save)
        }
    FavouriteToggle(
        isFavourite = save.isSaved,
        onToggle = { isSaved -> if (isSaved) asking = true else onSave(null) },
        label = label,
    )
    if (asking) {
        AliasDialog(
            title = stringResource(R.string.stop_detail_save_title),
            initial = save.alias,
            onDismiss = { asking = false },
            onConfirm = { alias ->
                asking = false
                onSave(alias)
            },
        )
    }
}

/**
 * FR-STOP-08: the alias field, which stops at [SavedStopAlias.MAX_LENGTH] characters as the user types, so nothing is
 * cut behind the user's back. A blank field saves the stop under its own name.
 */
@Composable
fun AliasDialog(
    title: String,
    initial: String?,
    onDismiss: () -> Unit,
    onConfirm: (alias: String?) -> Unit,
) {
    var text by remember { mutableStateOf(initial.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                // Typing is kept as typed while it fits, so a trailing space is not eaten mid-name; past the limit the
                // field holds the trimmed alias, which is what the screen will show.
                onValueChange = { typed ->
                    text =
                        if (typed.length <= SavedStopAlias.MAX_LENGTH) {
                            typed
                        } else {
                            SavedStopAlias.of(typed).orEmpty()
                        }
                },
                label = { Text(stringResource(R.string.stop_detail_save_alias, SavedStopAlias.MAX_LENGTH)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(SavedStopAlias.of(text)) }) {
                Text(stringResource(R.string.stop_detail_save_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.stop_detail_save_cancel)) }
        },
    )
}
