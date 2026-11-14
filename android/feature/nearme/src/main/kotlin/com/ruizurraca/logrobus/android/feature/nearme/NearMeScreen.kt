package com.ruizurraca.logrobus.android.feature.nearme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruizurraca.logrobus.android.core.designsystem.EtaText
import com.ruizurraca.logrobus.android.core.designsystem.LineBadge
import com.ruizurraca.logrobus.android.core.designsystem.LogroBusTheme
import com.ruizurraca.logrobus.android.core.designsystem.MoreActionButton
import com.ruizurraca.logrobus.android.core.designsystem.ScreenTitle
import com.ruizurraca.logrobus.android.core.designsystem.Tokens
import com.ruizurraca.logrobus.android.core.designsystem.etaSpoken
import com.ruizurraca.logrobus.core.presentation.EtaUi
import com.ruizurraca.logrobus.feature.nearme.presentation.NearMeEvent
import com.ruizurraca.logrobus.feature.nearme.presentation.NearMeUiState
import com.ruizurraca.logrobus.feature.nearme.presentation.NearYouUi
import com.ruizurraca.logrobus.feature.nearme.presentation.NearbyStopRowUi
import com.ruizurraca.logrobus.feature.nearme.presentation.SavedStopRowUi
import org.koin.androidx.compose.koinViewModel

/**
 * Near Me with its ViewModel from the graph (ADR-0014): the root of the Near Me tab (LB-201). The location updates run
 * while it shows, and stop when another tab is selected or the app goes to the background (FR-LOC-03).
 */
@Composable
fun NearMeRoute(
    onOpenMore: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenStop: (stopId: Int) -> Unit = {},
    editViewModel: MyStopsEditViewModel = koinViewModel(),
    viewModel: NearMeViewModel = koinViewModel(),
) {
    val editing = remember { mutableStateOf(false) }
    val rows by editViewModel.rows.collectAsStateWithLifecycle()
    if (editing.value) {
        MyStopsEditSheet(
            rows = rows,
            onReorder = editViewModel::reorder,
            onRename = editViewModel::rename,
            onDelete = editViewModel::delete,
            onDismiss = { editing.value = false },
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val turnOnLocation = rememberLocationRequest { viewModel.onEvent(NearMeEvent.PermissionMayHaveChanged) }
    // Back from the system's settings, where the user may have turned location on.
    LifecycleResumeEffect(viewModel) {
        viewModel.onEvent(NearMeEvent.PermissionMayHaveChanged)
        onPauseOrDispose {}
    }
    NearMeScreen(
        state,
        onTurnOnLocation = turnOnLocation,
        onOpenMore = onOpenMore,
        modifier = modifier,
        onOpenStop = onOpenStop,
        onEditMyStops = { editing.value = true },
    )
}

/**
 * Renders [state] under its top app bar, with the More action (FR-NAV-04). Until the map (LB-202) and the sheet
 * (LB-203) land, it shows the "Near you" section only, and only without location (owner decision, 2026-11-05).
 */
@Suppress("LongParameterList") // The state, its three actions, the modifier, and the two doors out of the sections.
@Composable
fun NearMeScreen(
    state: NearMeUiState,
    onTurnOnLocation: () -> Unit,
    onOpenMore: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenStop: (stopId: Int) -> Unit = {},
    onEditMyStops: () -> Unit = {},
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
            // FR-HOME-02 and FR-NEAR-03: "My stops" sits above "Near you". An empty section says how to fill it,
            // with the action that will focus the search (LB-209).
            item { MyStopsHeading(onEdit = onEditMyStops, canEdit = !state.myStops.isEmpty) }
            if (state.myStops.isEmpty) {
                item { MyStopsEmptyHint() }
            } else {
                items(state.myStops.rows, key = { it.stopId }) { row -> MyStopRow(row, onOpenStop) }
            }
            if (state.nearYou == NearYouUi.TurnOnLocation) {
                item { NearYouHeading() }
                item { TurnOnLocationRow(onTurnOnLocation) }
            }
            // FR-HOME-03 and NFR-A11Y-04: the list is the map's equivalent, so every stop is reachable and openable
            // here without touching the map. Until the first position arrives there is nothing to list, and the
            // section shows no empty heading either.
            val section = state.nearYou as? NearYouUi.LocationOn
            if (section != null && section.stops.isNotEmpty()) {
                item { NearYouHeading() }
                items(section.stops, key = { it.stopId }) { stop ->
                    NearbyStopRow(stop, section.isRefined, onOpenStop)
                }
            }
        }
    }
}

/**
 * FR-HOME-02: the section title, with FR-FAV-02's Edit action while there is something to edit. Edit opens the sheet
 * that reorders, renames and removes the saved stops.
 */
@Composable
private fun MyStopsHeading(
    onEdit: () -> Unit,
    canEdit: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            SectionHeading(stringResource(R.string.nearme_my_stops_title))
        }
        if (canEdit) {
            TextButton(onClick = onEdit) { Text(stringResource(R.string.nearme_my_stops_edit)) }
        }
    }
}

/** FR-HOME-02: what the section says when nothing is saved. */
@Composable
private fun MyStopsEmptyHint() {
    ListItem(
        headlineContent = { Text(stringResource(R.string.nearme_my_stops_empty)) },
    )
}

/**
 * FR-FAV-02 and NFR-A11Y-02: one saved stop, under its alias, read as one utterance and opened with a double tap.
 */
@Composable
private fun MyStopRow(
    row: SavedStopRowUi,
    onOpenStop: (Int) -> Unit,
) {
    // LB-204: the stop's next calls under its name, each worded by the design system's EtaText as the stop detail words
    // one. One utterance: TalkBack reads the stop and its calls together, not four nodes in a row.
    val resources = LocalResources.current
    val spoken =
        buildList {
            add(row.label)
            row.arrivals.forEach { arrival ->
                val eta =
                    arrival.eta?.let { display ->
                        EtaUi(
                            display,
                            arrival.isLowConfidence,
                            scheduled = false,
                            delay = null,
                            cancelled = arrival.isCancelled,
                        )
                    }
                eta?.let { add(resources.etaSpoken(it)) }
            }
        }.joinToString(", ")
    ListItem(
        headlineContent = { Text(row.label, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = {
            row.arrivals.forEach { arrival ->
                val eta =
                    arrival.eta?.let { display ->
                        EtaUi(
                            display,
                            arrival.isLowConfidence,
                            scheduled = false,
                            delay = null,
                            cancelled = arrival.isCancelled,
                        )
                    }
                if (eta != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LineBadge(arrival.lineCode, arrival.color, arrival.onColor)
                        EtaText(eta, Modifier.padding(start = Tokens.Spacing.s))
                    }
                }
            }
        },
        modifier =
            Modifier
                .clickable(onClickLabel = stringResource(R.string.nearme_nearby_open_stop), role = Role.Button) {
                    onOpenStop(row.stopId)
                }.clearAndSetSemantics { contentDescription = spoken },
    )
}

/** A section's title, in the primary colour and as a heading, so TalkBack can navigate by it. */
@Composable
private fun SectionHeading(text: String) {
    ListItem(
        headlineContent = {
            Text(
                text,
                modifier = Modifier.semantics { heading() },
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall,
            )
        },
    )
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

/**
 * FR-HOME-03: one stop of "Near you", read as one utterance, "Gonzalo de Berceo, 2 min a pie", and opened with a
 * double tap (NFR-A11Y-02). A walked distance shows the minutes API-08 gave; a straight line says the metres, because a
 * walk it does not know must not be invented.
 */
@Composable
private fun NearbyStopRow(
    stop: NearbyStopRowUi,
    isRefined: Boolean,
    onOpenStop: (Int) -> Unit,
) {
    val walk = stop.walkingMinutes
    val distance =
        if (isRefined && walk != null) {
            stringResource(R.string.nearme_nearby_walk_minutes, walk)
        } else {
            stringResource(R.string.nearme_nearby_distance_meters, stop.distanceMeters)
        }
    val spoken = stringResource(R.string.nearme_nearby_stop_spoken, stop.name, distance)
    ListItem(
        headlineContent = { Text(stop.name, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = { Text(distance, style = MaterialTheme.typography.bodyMedium) },
        modifier =
            Modifier
                .clickable(onClickLabel = stringResource(R.string.nearme_nearby_open_stop), role = Role.Button) {
                    onOpenStop(stop.stopId)
                }.clearAndSetSemantics { contentDescription = spoken },
    )
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun NearMeTurnOnLocationPreview() {
    LogroBusTheme { NearMeScreen(NearMePreviewData.turnOnLocation, onTurnOnLocation = {}, onOpenMore = {}) }
}

@PreviewLightDark
@Composable
private fun NearMeLocationOnPreview() {
    LogroBusTheme { NearMeScreen(NearMePreviewData.locationOn, onTurnOnLocation = {}, onOpenMore = {}) }
}
