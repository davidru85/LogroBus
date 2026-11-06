package com.ruizurraca.logrobus.android.feature.planner

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
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerUiState
import org.koin.androidx.compose.koinViewModel

/** How to Get There's root with its ViewModel from the graph (ADR-0014). Stubbed until the green phase. */
@Composable
fun PlannerRoute(
    onOpenMore: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlannerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PlannerScreen(state, onOpenMore = onOpenMore, modifier = modifier)
}

/** Renders [state] under its top app bar, with the More action. Stubbed until the green phase. */
@Composable
fun PlannerScreen(
    @Suppress("UnusedParameter") // Used by the green phase.
    state: PlannerUiState,
    onOpenMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { ScreenTitle(stringResource(R.string.plan_title)) },
                actions = { MoreActionButton(onClick = onOpenMore) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding))
    }
}
