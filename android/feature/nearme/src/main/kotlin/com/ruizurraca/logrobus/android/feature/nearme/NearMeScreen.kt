package com.ruizurraca.logrobus.android.feature.nearme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruizurraca.logrobus.android.core.designsystem.LogroBusTheme
import com.ruizurraca.logrobus.android.core.designsystem.MoreActionButton
import com.ruizurraca.logrobus.android.core.designsystem.ScreenTitle
import com.ruizurraca.logrobus.feature.nearme.presentation.NearMeEvent
import com.ruizurraca.logrobus.feature.nearme.presentation.NearMeUiState
import com.ruizurraca.logrobus.feature.nearme.presentation.NearYouUi
import org.koin.androidx.compose.koinViewModel

/**
 * Near Me with its ViewModel from the graph (ADR-0014): the root of the Near Me tab (LB-201). The location updates run
 * while it shows, and stop when another tab is selected or the app goes to the background (FR-LOC-03).
 */
@Composable
fun NearMeRoute(
    onOpenMore: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NearMeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val turnOnLocation = rememberLocationRequest { viewModel.onEvent(NearMeEvent.PermissionMayHaveChanged) }
    // Back from the system's settings, where the user may have turned location on.
    LifecycleResumeEffect(viewModel) {
        viewModel.onEvent(NearMeEvent.PermissionMayHaveChanged)
        onPauseOrDispose {}
    }
    NearMeScreen(state, onTurnOnLocation = turnOnLocation, onOpenMore = onOpenMore, modifier = modifier)
}

/**
 * Renders [state] under its top app bar, with the More action (FR-NAV-04). Until the map (LB-202) and the sheet
 * (LB-203) land, it shows the "Near you" section only, and only without location (owner decision, 2026-11-05).
 */
@Composable
fun NearMeScreen(
    state: NearMeUiState,
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().consumeWindowInsets(padding),
            contentPadding = padding,
        ) {
            if (state.nearYou == NearYouUi.TurnOnLocation) {
                item { NearYouHeading() }
                item { TurnOnLocationRow(onTurnOnLocation) }
            }
        }
    }
}

@Composable
private fun NearYouHeading() {
    ListItem(
        headlineContent = {
            Text(
                stringResource(R.string.nearme_nearby_title),
                modifier = Modifier.semantics { heading() },
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall,
            )
        },
    )
}

/** FR-NEAR-06: the one row of "Near you" without location, which opens the permission flow (FR-LOC-01). */
@Composable
private fun TurnOnLocationRow(onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.nearme_nearby_turn_on_location)) },
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
        // The text says it all; the icon is decoration.
        leadingContent = { Icon(painterResource(R.drawable.ic_location), contentDescription = null) },
    )
}

@Preview(name = "Turn on location")
@Composable
private fun NearMeTurnOnLocationPreview() {
    LogroBusTheme { NearMeScreen(NearMePreviewData.turnOnLocation, onTurnOnLocation = {}, onOpenMore = {}) }
}

@Preview(name = "Location on")
@Composable
private fun NearMeLocationOnPreview() {
    LogroBusTheme { NearMeScreen(NearMePreviewData.locationOn, onTurnOnLocation = {}, onOpenMore = {}) }
}
