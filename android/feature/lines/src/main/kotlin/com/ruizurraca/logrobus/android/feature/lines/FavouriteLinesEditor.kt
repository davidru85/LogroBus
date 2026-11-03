package com.ruizurraca.logrobus.android.feature.lines

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ruizurraca.logrobus.feature.lines.presentation.EditableFavouriteLineUi

@Suppress("UnusedParameter")
@Composable
internal fun FavouriteLinesEditor(
    rows: List<EditableFavouriteLineUi>,
    onReorder: (lineIds: List<Int>) -> Unit,
    onRemove: (lineId: Int) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) = Unit

internal const val FAVOURITE_LINES_EDITOR = "favouriteLinesEditor"

internal fun editRowTag(lineId: Int) = "editRow:$lineId"

internal fun dragHandleTag(lineId: Int) = "dragHandle:$lineId"
