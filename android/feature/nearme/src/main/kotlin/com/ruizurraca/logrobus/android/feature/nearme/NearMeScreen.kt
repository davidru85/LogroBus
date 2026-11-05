package com.ruizurraca.logrobus.android.feature.nearme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruizurraca.logrobus.android.core.designsystem.MoreActionButton
import com.ruizurraca.logrobus.android.core.designsystem.ScreenTitle
import com.ruizurraca.logrobus.feature.nearme.presentation.NearMeUiState
import org.koin.androidx.compose.koinViewModel

/** Near Me with its ViewModel from the graph (ADR-0014): the root of the Near Me tab (LB-201). */
@Composable
fun NearMeRoute(
    onOpenMore: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NearMeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NearMeScreen(state, onTurnOnLocation = {}, onOpenMore = onOpenMore, modifier = modifier)
}

/** Renders [state] under its top app bar, with the More action (FR-NAV-04). */
@Composable
fun NearMeScreen(
    @Suppress("UnusedParameter") // Used by the green phase.
    state: NearMeUiState,
    @Suppress("UnusedParameter") // Used by the green phase.
    onTurnOnLocation: () -> Unit,
    onOpenMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { ScreenTitle(stringResource(R.string.nearme_title)) },
                actions = { MoreActionButton(onClick = onOpenMore) },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        )
    }
}
