package com.ruizurraca.logrobus.android.feature.lines

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.pluralStringResource

/**
 * FR-FAV-05: a star tapped with [limit] favourite lines saved shows why nothing was saved, in [snackbars], then calls
 * [onShown] so it shows once. A snackbar is announced by TalkBack without taking its focus, and stays while TalkBack
 * needs it to (NFR-A11Y).
 */
@Composable
internal fun FavouriteLimitMessage(
    limit: Int?,
    snackbars: SnackbarHostState,
    onShown: () -> Unit,
) {
    val message = limit?.let { pluralStringResource(R.plurals.lines_favourites_limit, it, it) }
    val shown by rememberUpdatedState(onShown)
    LaunchedEffect(message) {
        if (message != null) {
            snackbars.showSnackbar(message, withDismissAction = true, duration = SnackbarDuration.Long)
            shown()
        }
    }
}
