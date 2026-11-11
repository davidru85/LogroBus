package com.ruizurraca.logrobus.android.feature.stops

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruizurraca.logrobus.android.core.designsystem.EtaText
import com.ruizurraca.logrobus.android.core.designsystem.LineBadge
import com.ruizurraca.logrobus.android.core.designsystem.LogroBusTheme
import com.ruizurraca.logrobus.android.core.designsystem.RetryMessage
import com.ruizurraca.logrobus.android.core.designsystem.ScreenTitle
import com.ruizurraca.logrobus.android.core.designsystem.Tokens
import com.ruizurraca.logrobus.android.core.designsystem.UpButton
import com.ruizurraca.logrobus.android.core.designsystem.etaSpoken
import com.ruizurraca.logrobus.android.core.designsystem.messageRes
import com.ruizurraca.logrobus.core.presentation.EtaUi
import com.ruizurraca.logrobus.feature.stops.presentation.ServedArrivalUi
import com.ruizurraca.logrobus.feature.stops.presentation.StopDetailUiState
import com.ruizurraca.logrobus.feature.stops.presentation.StopUpdatedUi
import com.ruizurraca.logrobus.transit.domain.ServedLine
import com.ruizurraca.logrobus.transit.domain.Stop
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import com.ruizurraca.logrobus.android.core.designsystem.R as DS

/**
 * The detail of stop [stopId] (SC-05, FR-STOP-03 to FR-STOP-05), with its ViewModel from the graph (ADR-0014).
 * [onBack] goes back. A pushed screen from line detail, and the same screen in Near Me's sheet (FR-NEAR-04,
 * FR-NAV-07).
 */
@Composable
fun StopDetailRoute(
    stopId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StopDetailViewModel = koinViewModel { parametersOf(stopId) },
    savedViewModel: SavedStopViewModel = koinViewModel { parametersOf(stopId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val save by savedViewModel.state.collectAsStateWithLifecycle()
    StopDetailScreen(state, viewModel::retry, onBack, modifier, save, savedViewModel::save)
}

/**
 * A stop's detail (SC-05): its name and the lines that serve it with their headsigns (FR-STOP-03, DD-16), then its
 * arrivals (FR-STOP-04), how old the list is (FR-STOP-05), and Retry or "Tap to refresh" (FR-ERR-05).
 *
 * The ETAs are the design system's [EtaText], which draws DD-09's "~", "Now" and the cancelled row's strike-through
 * with its label, so a row never words an ETA itself (design_guidelines.md §6.3). Each row is one utterance, built
 * from [etaSpoken], so the wording has no second copy.
 */
@Suppress("LongParameterList") // The state, its retry, the way back, the modifier and the save action.
@Composable
fun StopDetailScreen(
    state: StopDetailUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    save: SavedStopUi? = null,
    onSave: (alias: String?) -> Unit = {},
) {
    val stop = (state as? StopDetailUiState.Content)?.stop
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                // "Stop" until the name is known, so the screen is never untitled.
                title = { ScreenTitle(stop?.name ?: stringResource(R.string.stop_detail_title_generic)) },
                navigationIcon = { UpButton(onClick = onBack) },
                // FR-STOP-08: a saved stop offers its alias; an unsaved one offers Save, which asks for it.
                actions = { save?.let { SaveAction(it, onSave) } },
            )
        },
    ) { padding ->
        val centred = Modifier.padding(padding).consumeWindowInsets(padding)
        when (state) {
            StopDetailUiState.Loading -> {
                LoadingStop(padding)
            }

            is StopDetailUiState.Content -> {
                StopDetailBody(state, padding, onRetry)
            }

            is StopDetailUiState.Error -> {
                RetryMessage(stringResource(state.reason.messageRes), onRetry, centred)
            }
        }
    }
}

/**
 * Spec §7.3: the header's shape, the name and the line badges, never a lone spinner. TalkBack hears "Loading stop"
 * (design_guidelines.md §8).
 */
@Composable
private fun LoadingStop(contentPadding: PaddingValues) {
    val loading = stringResource(R.string.stop_detail_loading)
    ListItem(
        headlineContent = { Text(stringResource(R.string.stop_detail_title_generic)) },
        modifier =
            Modifier
                .consumeWindowInsets(contentPadding)
                .padding(contentPadding)
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = loading },
    )
}

/** The stop's header, the age of its list, and its arrivals (FR-STOP-03 to FR-STOP-05). */
@Composable
private fun StopDetailBody(
    state: StopDetailUiState.Content,
    contentPadding: PaddingValues,
    onRetry: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.consumeWindowInsets(contentPadding),
        contentPadding = contentPadding,
    ) {
        item { StopHeader(state.stop) }
        state.updated?.let { updated ->
            item { UpdatedLine(updated, onRetry) }
        }
        items(state.arrivals) { arrival -> ArrivalRow(arrival) }
    }
}

/**
 * The stop's name and the lines that serve it, each with its badge and the headsign of the pattern that reaches it
 * (FR-STOP-03, DD-16). A column of rows rather than chips, so a headsign wraps instead of being cut short at 200 %
 * (LB-406). TalkBack reads each served line as one: "Line 1, towards Hospital San Pedro".
 */
@Composable
private fun StopHeader(stop: Stop) {
    Column(Modifier.padding(vertical = Tokens.Spacing.s)) {
        Text(
            stop.name,
            modifier =
                Modifier
                    .padding(horizontal = Tokens.Spacing.l)
                    .semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        stop.servedLines.forEach { line -> ServedLineRow(line) }
    }
}

@Composable
private fun ServedLineRow(line: ServedLine) {
    val badge = stringResource(DS.string.line_badge, line.code)
    val spoken =
        line.headsign?.let { headsign ->
            stringResource(
                R.string.stop_detail_served_line_spoken,
                badge,
                stringResource(R.string.stop_detail_towards, headsign),
            )
        } ?: badge
    Row(
        Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = spoken }
            .padding(horizontal = Tokens.Spacing.l, vertical = Tokens.Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LineBadge(line.code, line.color, line.onColor)
        line.headsign?.let { headsign ->
            Text(stringResource(R.string.stop_detail_towards, headsign), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * FR-STOP-05 (design_guidelines.md §7): "Updated N s ago" in the warning colour from 50 s, and past 120 s the list's
 * ETAs are clock times and the screen offers "Tap to refresh", which [onRetry] runs.
 */
@Composable
private fun UpdatedLine(
    updated: StopUpdatedUi,
    onRetry: () -> Unit,
) {
    if (!updated.isStale && !updated.needsRefresh) return
    Column(Modifier.padding(horizontal = Tokens.Spacing.l)) {
        Text(
            stringResource(R.string.stop_detail_updated_ago, updated.ageSeconds),
            color = LogroBusTheme.statusColors.warning,
            style = MaterialTheme.typography.bodySmall,
        )
        if (updated.needsRefresh) {
            TextButton(onClick = onRetry) { Text(stringResource(R.string.stop_detail_tap_to_refresh)) }
        }
    }
}

/**
 * One arrival of the stop (FR-STOP-04, spec §5.7): its line's badge, the headsign of the journey it belongs to
 * (DD-16), and its ETA. The ETA is [EtaText], which draws DD-09's "~", "Now" and a cancelled arrival's strike-through
 * with its "Cancelled" label (design_guidelines.md §6.3). A row the mapper could not place shows the criterion's dash,
 * read as "no calls". TalkBack hears the row as one utterance.
 */
@Composable
private fun ArrivalRow(arrival: ServedArrivalUi) {
    val resources = LocalResources.current
    val badge = stringResource(DS.string.line_badge, arrival.lineCode)
    val headsign =
        arrival.headsign?.let { stringResource(R.string.stop_detail_towards, it) }
    val eta =
        arrival.eta?.let { display ->
            EtaUi(display, arrival.isLowConfidence, scheduled = false, delay = null, cancelled = arrival.isCancelled)
        }
    val call = eta?.let { resources.etaSpoken(it) } ?: stringResource(R.string.stop_detail_eta_none_spoken)
    val spoken =
        headsign?.let { stringResource(R.string.stop_detail_arrival_headsign_spoken, badge, it, call) }
            ?: stringResource(R.string.stop_detail_arrival_spoken, badge, call)
    ListItem(
        headlineContent = {
            // DD-16: the headsign tells apart the two directions of the same line. It wraps at 200 %.
            headsign?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        },
        leadingContent = { LineBadge(arrival.lineCode, arrival.color, arrival.onColor) },
        trailingContent = {
            if (eta == null) {
                Text(
                    stringResource(R.string.stop_detail_eta_none),
                    // The dash is the row's content, as on iOS: the primary colour, not the muted one a cancelled ETA
                    // takes (NFR-A11Y-01).
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
            } else {
                EtaText(eta, Modifier.padding(vertical = Tokens.Spacing.s))
            }
        },
        modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
    )
}
