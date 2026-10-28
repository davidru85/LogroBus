package com.ruizurraca.logrobus.android.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.presentation.MadridDateTime
import com.ruizurraca.logrobus.feature.settings.domain.DatasetSync
import com.ruizurraca.logrobus.feature.settings.presentation.AboutDataUiMapper
import com.ruizurraca.logrobus.feature.settings.presentation.AboutDataUiState
import com.ruizurraca.logrobus.feature.settings.presentation.LastSyncUi
import com.ruizurraca.logrobus.transit.domain.StaticDataset
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Instant

/** About data with its ViewModel from the graph (ADR-0014). More opens it, once the app shell lands (LB-012). */
@Composable
fun AboutDataRoute(
    modifier: Modifier = Modifier,
    viewModel: AboutDataViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AboutDataScreen(state, modifier)
}

/** Renders [state] and nothing else. The data source, disclaimer and attributions come with LB-404. */
@Composable
fun AboutDataScreen(
    state: AboutDataUiState,
    modifier: Modifier = Modifier,
) {
    when (state) {
        AboutDataUiState.Loading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }

        is AboutDataUiState.Content -> {
            LastSyncs(state.lastSyncs, modifier)
        }

        is AboutDataUiState.Error -> {}
    }
}

@Composable
private fun LastSyncs(
    lastSyncs: List<LastSyncUi>,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
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
                    Text(sync.lastSuccess?.let { formatted(it) } ?: stringResource(R.string.about_last_sync_never))
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

/** The Madrid wall-clock time in the device locale's medium date and short time (AGENTS.md §5.5). */
@Composable
private fun formatted(time: MadridDateTime): String =
    LocalDateTime
        .of(time.year, time.month, time.day, time.hour, time.minute)
        .format(
            DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(LocalConfiguration.current.locales[0]),
        )

private val previewSync = Instant.parse("2026-10-28T09:30:00Z")

@Preview(showBackground = true)
@Composable
private fun AboutDataLoadingPreview() {
    MaterialTheme { AboutDataScreen(AboutDataUiState.Loading) }
}

@Preview(showBackground = true)
@Composable
private fun AboutDataSyncedPreview() {
    MaterialTheme {
        AboutDataScreen(
            AboutDataUiMapper.state(AppResult.Success(StaticDataset.entries.map { DatasetSync(it, previewSync) })),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutDataNeverSyncedPreview() {
    MaterialTheme {
        AboutDataScreen(AboutDataUiMapper.state(AppResult.Success(StaticDataset.entries.map { DatasetSync(it, null) })))
    }
}
