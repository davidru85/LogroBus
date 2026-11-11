package com.ruizurraca.logrobus.android.feature.lines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruizurraca.logrobus.android.core.designsystem.EtaText
import com.ruizurraca.logrobus.android.core.designsystem.FavouriteToggle
import com.ruizurraca.logrobus.android.core.designsystem.LineBadge
import com.ruizurraca.logrobus.android.core.designsystem.LineBadgeSize
import com.ruizurraca.logrobus.android.core.designsystem.RetryMessage
import com.ruizurraca.logrobus.android.core.designsystem.ScreenTitle
import com.ruizurraca.logrobus.android.core.designsystem.UpButton
import com.ruizurraca.logrobus.android.core.designsystem.etaSpoken
import com.ruizurraca.logrobus.android.core.designsystem.messageRes
import com.ruizurraca.logrobus.core.presentation.EtaUi
import com.ruizurraca.logrobus.feature.lines.presentation.DirectionOptionUi
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailEvent
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailUi
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailUiState
import com.ruizurraca.logrobus.feature.lines.presentation.StopArrivalUi
import com.ruizurraca.logrobus.feature.lines.presentation.StopRowUi
import com.ruizurraca.logrobus.transit.domain.Direction
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/** The detail of line [lineId] (SC-04), with its ViewModel from the graph (ADR-0014). [onBack] goes back. */
@Composable
fun LineDetailRoute(
    lineId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LineDetailViewModel = koinViewModel { parametersOf(lineId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val favouriteLimit by viewModel.favouriteLimit.collectAsStateWithLifecycle()
    val arrivals by viewModel.arrivals.collectAsStateWithLifecycle()
    LineDetailScreen(state, viewModel::onEvent, onBack, modifier, favouriteLimit, arrivals)
}

/**
 * A line's detail: the line and its favourite toggle (LB-103, FR-LINE-09), the direction toggle and the selected
 * direction's ordered stop list (LB-104, FR-LINE-04), each stop with its next predicted call or a dash (LB-105,
 * FR-LINE-05). The map of both shapes waits for LB-013. The toolbar names the line and holds the toggle, labelled as
 * the rows' are.
 */
@Suppress("LongParameterList") // The state and its events, the way back, the modifier, the limit message and the calls.
@Composable
fun LineDetailScreen(
    state: LineDetailUiState,
    onEvent: (LineDetailEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    favouriteLimit: Int? = null,
    arrivals: Map<Int, StopArrivalUi> = emptyMap(),
    onOpenStop: (stopId: Int) -> Unit = {},
) {
    val line = (state as? LineDetailUiState.Content)?.line
    val snackbars = remember { SnackbarHostState() }
    FavouriteLimitMessage(favouriteLimit, snackbars) { onEvent(LineDetailEvent.FavouriteLimitShown) }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbars) },
        topBar = {
            TopAppBar(
                // "Line" until the line's code is known, so the screen is never untitled.
                title = {
                    val title = line?.let { stringResource(R.string.line_detail_title, it.code) }
                    ScreenTitle(title ?: stringResource(R.string.line_detail_title_generic))
                },
                navigationIcon = { UpButton(onClick = onBack) },
                actions = { line?.let { FavouriteAction(it, onEvent) } },
            )
        },
    ) { padding ->
        val centred = Modifier.padding(padding).consumeWindowInsets(padding)
        when (state) {
            LineDetailUiState.Loading -> {
                LoadingLine(padding)
            }

            is LineDetailUiState.Content -> {
                LineDetailBody(state, padding, arrivals, onEvent, onOpenStop)
            }

            is LineDetailUiState.Error -> {
                RetryMessage(stringResource(state.reason.messageRes), { onEvent(LineDetailEvent.Retry) }, centred)
            }
        }
    }
}

@Composable
private fun FavouriteAction(
    line: LineDetailUi,
    onEvent: (LineDetailEvent) -> Unit,
) {
    val label = if (line.isFavourite) R.string.lines_favourite_remove else R.string.lines_favourite_add
    FavouriteToggle(
        isFavourite = line.isFavourite,
        onToggle = { onEvent(LineDetailEvent.SetFavourite(it)) },
        label = stringResource(label, line.code),
    )
}

/** The line's badge and its whole display name, which wraps. TalkBack reads them as one (NFR-A11Y-02). */
@Composable
private fun LineHeader(line: LineDetailUi) {
    ListItem(
        headlineContent = { Text(line.displayName, style = MaterialTheme.typography.titleMedium) },
        modifier = Modifier.semantics(mergeDescendants = true) {},
        leadingContent = { LineBadge(line.code, line.color, line.onColor, size = LineBadgeSize.Large) },
    )
}

/**
 * Spec §7.3: the header's shape, a badge and a name, never a lone spinner. TalkBack hears "Loading line"
 * (design_guidelines.md §8).
 */
@Composable
private fun LoadingLine(contentPadding: PaddingValues) {
    val loading = stringResource(R.string.line_detail_loading)
    val lineHeight =
        with(LocalDensity.current) {
            MaterialTheme.typography.titleMedium.lineHeight
                .toDp()
        }
    val badge = LineBadgeSize.Large
    ListItem(
        headlineContent = { Placeholder(Modifier.fillMaxWidth(PLACEHOLDER_WIDTH).height(lineHeight)) },
        modifier =
            Modifier
                .consumeWindowInsets(contentPadding)
                .padding(contentPadding)
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = loading },
        leadingContent = { Placeholder(Modifier.size(badge.minWidth, badge.minHeight)) },
    )
}

/**
 * The line's header, the direction toggle and the ordered stop list (LB-104, FR-LINE-04), each stop with its next call
 * (LB-105, FR-LINE-05). A lazy column, so a long pattern composes only the rows it shows.
 */
@Composable
private fun LineDetailBody(
    state: LineDetailUiState.Content,
    contentPadding: PaddingValues,
    arrivals: Map<Int, StopArrivalUi>,
    onEvent: (LineDetailEvent) -> Unit,
    onOpenStop: (stopId: Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.consumeWindowInsets(contentPadding),
        contentPadding = contentPadding,
    ) {
        item { LineHeader(state.line) }
        if (state.directions.isNotEmpty()) {
            item {
                DirectionToggle(
                    options = state.directions,
                    onSelect = { onEvent(LineDetailEvent.SelectDirection(it)) },
                )
            }
        }
        items(state.stops) { stop -> StopRow(stop, arrivals[stop.stopId], onOpen = { onOpenStop(stop.stopId) }) }
    }
}

/**
 * FR-LINE-04 and DD-16: one option per direction, each read as "Towards <headsign>". A column of full-width selectable
 * rows rather than chips: a chip keeps its text on one line, and "Hacia Hospital San Pedro" is cut short at 200 %
 * (LB-406). A row wraps, and `Modifier.selectable` tells the screen reader which one is selected.
 */
@Composable
private fun DirectionToggle(
    options: List<DirectionOptionUi>,
    onSelect: (Direction) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .padding(horizontal = DETAIL_CONTENT_PADDING.dp)
                .selectableGroup(),
    ) {
        options.forEach { option ->
            val label = stringResource(R.string.lines_towards, option.headsign)
            ListItem(
                headlineContent = { Text(label, style = MaterialTheme.typography.bodyLarge) },
                leadingContent = {
                    RadioButton(selected = option.isSelected, onClick = null)
                },
                modifier =
                    Modifier
                        .selectable(
                            selected = option.isSelected,
                            role = Role.RadioButton,
                            onClick = { onSelect(option.direction) },
                        ),
            )
        }
    }
}

/**
 * One stop of the pattern, numbered as the screen reader reads it: "Stop 3, Varea", then its next call, "in 4 minutes"
 * (LB-104, LB-105). The call is the design system's ETA, which draws the "~" of a low-confidence one, the "Now" pulse
 * and the cancelled row's strike-through and label, so this row never words an ETA itself (design_guidelines.md §6.3).
 * A stop with no call shows the criterion's dash.
 */
@Composable
private fun StopRow(
    stop: StopRowUi,
    arrival: StopArrivalUi?,
    onOpen: () -> Unit,
) {
    val position = stringResource(R.string.line_detail_stop_position, stop.order, stop.name)
    val eta =
        arrival?.let { call ->
            call.eta?.let { display ->
                EtaUi(display, call.isLowConfidence, scheduled = false, delay = null, cancelled = call.isCancelled)
            }
        }
    val resources = LocalResources.current
    // The dash is read as "no calls", not as a silence: a rider who hears only the stop cannot tell it is empty.
    val call = eta?.let { resources.etaSpoken(it) } ?: stringResource(R.string.line_detail_eta_none_spoken)
    val spoken = stringResource(R.string.line_detail_stop_eta, position, call)
    ListItem(
        headlineContent = { Text(stop.name, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = { Text(stop.order.toString(), style = MaterialTheme.typography.labelLarge) },
        trailingContent = {
            if (eta == null) {
                Text(
                    stringResource(R.string.line_detail_eta_none),
                    // The dash is the row's content, as on iOS: the primary colour, not the muted one a cancelled or
                    // scheduled ETA takes (NFR-A11Y-01).
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
            } else {
                EtaText(eta)
            }
        },
        modifier =
            Modifier
                .clickable(onClickLabel = stringResource(R.string.line_detail_open_stop), role = Role.Button) { onOpen() }
                .clearAndSetSemantics { contentDescription = spoken },
    )
}

/** The detail's own inset, matching the list rows' (design_guidelines.md §4). */
private const val DETAIL_CONTENT_PADDING = 16
