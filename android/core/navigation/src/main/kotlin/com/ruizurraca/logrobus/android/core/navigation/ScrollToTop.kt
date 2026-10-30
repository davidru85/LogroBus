package com.ruizurraca.logrobus.android.core.navigation

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Re-selections of a tab that is already at its root, each asking the root to scroll to its top (FR-NAV-03). The app
 * shell provides them to every tab root; anywhere else there are none.
 */
val LocalScrollToTopRequests = staticCompositionLocalOf<Flow<Unit>> { emptyFlow() }

/** Scrolls [state] to its top whenever the root's tab is re-selected. A tab root with a scrolling list calls this. */
@Composable
fun ScrollToTopOnReselect(state: LazyListState) {
    val requests = LocalScrollToTopRequests.current
    LaunchedEffect(requests, state) {
        requests.collect { state.animateScrollToItem(0) }
    }
}
