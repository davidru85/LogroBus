package com.ruizurraca.logrobus.android.core.navigation

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

val LocalScrollToTopRequests = staticCompositionLocalOf<Flow<Unit>> { emptyFlow() }

@Composable
@Suppress("UNUSED_PARAMETER")
fun ScrollToTopOnReselect(state: LazyListState) {
}
