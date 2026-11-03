package com.ruizurraca.logrobus.android.core.designsystem

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * A modal bottom sheet for a task over a screen, such as editing the favourite lines (LB-110): it opens fully
 * expanded, and a swipe down, a tap outside or back closes it through [onDismissRequest]. [content] gets `close`, for
 * its own Done, which slides the sheet away and then calls [onDismissRequest].
 */
@OptIn(ExperimentalMaterial3Api::class) // Experimental Material APIs stay inside the design system (ADR-0005).
@Composable
fun ModalSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = { scope.launch { sheet.hide() }.invokeOnCompletion { onDismissRequest() } }
    ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheet) { content(close) }
}
