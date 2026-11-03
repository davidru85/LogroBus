package com.ruizurraca.logrobus.android.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruizurraca.logrobus.android.core.designsystem.RetryMessage
import com.ruizurraca.logrobus.android.core.designsystem.ScreenTitle
import com.ruizurraca.logrobus.android.core.designsystem.Tokens
import com.ruizurraca.logrobus.android.core.designsystem.UpButton
import com.ruizurraca.logrobus.android.core.designsystem.messageRes
import com.ruizurraca.logrobus.feature.settings.presentation.AboutDataEvent
import com.ruizurraca.logrobus.feature.settings.presentation.AboutDataUiState
import com.ruizurraca.logrobus.feature.settings.presentation.LastSyncUi
import com.ruizurraca.logrobus.transit.domain.StaticDataset
import org.koin.androidx.compose.koinViewModel

/** About data with its ViewModel from the graph (ADR-0014), opened from More (LB-012). [onBack] leaves it. */
@Composable
fun AboutDataRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AboutDataViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AboutDataScreen(state, viewModel::onEvent, onBack, modifier)
}

/**
 * Renders [state] under its top app bar and sends [onEvent]. The data source, disclaimer and attributions come with
 * LB-404.
 */
@Composable
fun AboutDataScreen(
    state: AboutDataUiState,
    onEvent: (AboutDataEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { ScreenTitle(stringResource(R.string.about_title)) },
                navigationIcon = { UpButton(onClick = onBack) },
            )
        },
    ) { padding ->
        AboutDataContent(state, onEvent, contentPadding = padding)
    }
}

@Composable
private fun AboutDataContent(
    state: AboutDataUiState,
    onEvent: (AboutDataEvent) -> Unit,
    contentPadding: PaddingValues,
) {
    when (state) {
        // Spec §7.3: the final layout with placeholders, never a lone spinner.
        AboutDataUiState.Loading -> {
            LastSyncs(StaticDataset.entries.map { LastSyncUi(it, null) }, loading = true, contentPadding)
        }

        is AboutDataUiState.Content -> {
            LastSyncs(state.lastSyncs, loading = false, contentPadding)
        }

        is AboutDataUiState.Error -> {
            RetryMessage(
                stringResource(state.reason.messageRes),
                onRetry = { onEvent(AboutDataEvent.Retry) },
                Modifier.padding(contentPadding).consumeWindowInsets(contentPadding),
            )
        }
    }
}

@Composable
private fun LastSyncs(
    lastSyncs: List<LastSyncUi>,
    loading: Boolean,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val format = rememberDateTimeFormat()
    // The padding goes inside the scroll, so the rows scroll behind the system bars rather than stop above them.
    Column(
        modifier
            .consumeWindowInsets(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        ListItem(
            headlineContent = {
                Text(
                    stringResource(R.string.about_last_sync_title),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall,
                )
            },
            modifier = Modifier.semantics { heading() },
        )
        lastSyncs.forEach { sync ->
            ListItem(
                headlineContent = { Text(stringResource(sync.dataset.labelRes)) },
                supportingContent = {
                    val lastSuccess = sync.lastSuccess
                    when {
                        loading -> TextPlaceholder()
                        lastSuccess == null -> Text(stringResource(R.string.about_last_sync_never))
                        else -> Text(format.format(lastSuccess))
                    }
                },
            )
        }
    }
}

@get:StringRes
private val StaticDataset.labelRes: Int
    get() =
        when (this) {
            StaticDataset.LINES -> R.string.about_dataset_lines
            StaticDataset.STOPS -> R.string.about_dataset_stops
        }
