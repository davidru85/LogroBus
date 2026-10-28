package com.ruizurraca.logrobus.android.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ruizurraca.logrobus.feature.settings.presentation.AboutDataUiState
import org.koin.androidx.compose.koinViewModel

/** About data with its ViewModel from the graph (ADR-0014). More opens it, once the app shell lands (LB-012). */
@Composable
fun AboutDataRoute(
    modifier: Modifier = Modifier,
    viewModel: AboutDataViewModel = koinViewModel(),
) {
    AboutDataScreen(AboutDataUiState.Loading, modifier)
}

/** Renders [state] and nothing else. */
@Composable
fun AboutDataScreen(
    state: AboutDataUiState,
    modifier: Modifier = Modifier,
) {
}
