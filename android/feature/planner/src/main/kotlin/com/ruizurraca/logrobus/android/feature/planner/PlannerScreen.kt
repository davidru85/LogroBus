package com.ruizurraca.logrobus.android.feature.planner

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruizurraca.logrobus.android.core.designsystem.LogroBusTheme
import com.ruizurraca.logrobus.android.core.designsystem.MoreActionButton
import com.ruizurraca.logrobus.android.core.designsystem.ScreenTitle
import com.ruizurraca.logrobus.android.core.designsystem.Tokens
import com.ruizurraca.logrobus.feature.planner.presentation.PlaceFieldUi
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerEvent
import com.ruizurraca.logrobus.feature.planner.presentation.PlannerUiState
import org.koin.androidx.compose.koinViewModel

/**
 * How to Get There's root with its ViewModel from the graph (ADR-0014): the root of the How to Get There tab
 * (LB-301). Each time it shows, the origin follows the location permission again (FR-PLAN-11).
 */
@Composable
fun PlannerRoute(
    onOpenMore: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlannerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Selected again, or back from the system's settings: the permission may have changed meanwhile.
    LifecycleResumeEffect(viewModel) {
        viewModel.onEvent(PlannerEvent.Shown)
        onPauseOrDispose {}
    }
    PlannerScreen(state, onOpenMore = onOpenMore, modifier = modifier)
}

/**
 * Renders [state] under its top app bar, with the More action (FR-NAV-04). Until place search lands (LB-301 part 2),
 * the origin and destination fields only, which open nothing yet (owner decision, 2026-11-06).
 */
@Composable
fun PlannerScreen(
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().consumeWindowInsets(padding),
            contentPadding = padding,
        ) {
            item { PlaceFields(state, Modifier.padding(horizontal = Tokens.Spacing.screenMargin)) }
        }
    }
}

/** The origin and destination fields, in one card (design_guidelines.md §3.3). */
@Composable
private fun PlaceFields(
    state: PlannerUiState,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(modifier.fillMaxWidth()) {
        PlaceField(
            icon = R.drawable.ic_trip_origin,
            field = state.origin,
            hint = R.string.plan_origin_hint,
            spoken = R.string.plan_origin_spoken,
        )
        HorizontalDivider()
        PlaceField(
            icon = R.drawable.ic_location_on,
            field = state.destination,
            hint = R.string.plan_destination_hint,
            spoken = R.string.plan_destination_spoken,
        )
    }
}

/** One field: what it shows, or its [hint] when empty, spoken after the field's name ([spoken]). */
@Composable
private fun PlaceField(
    @DrawableRes icon: Int,
    field: PlaceFieldUi,
    @StringRes hint: Int,
    @StringRes spoken: Int,
) {
    val shown =
        when (field) {
            PlaceFieldUi.Empty -> stringResource(hint)
            PlaceFieldUi.MyLocation -> stringResource(R.string.plan_my_location)
        }
    val description = stringResource(spoken, shown)
    // A hint in the secondary colour, as an empty text field's.
    val colour = if (field == PlaceFieldUi.Empty) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified
    ListItem(
        headlineContent = { Text(shown, color = colour) },
        // One stop for a screen reader, which says which field it is, then what it shows.
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
        // The field's name says it all; the icon is decoration.
        leadingContent = { Icon(painterResource(icon), contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun PlannerMyLocationPreview() {
    LogroBusTheme { PlannerScreen(PlannerPreviewData.myLocation, onOpenMore = {}) }
}

@PreviewLightDark
@Composable
private fun PlannerEmptyPreview() {
    LogroBusTheme { PlannerScreen(PlannerPreviewData.empty, onOpenMore = {}) }
}
