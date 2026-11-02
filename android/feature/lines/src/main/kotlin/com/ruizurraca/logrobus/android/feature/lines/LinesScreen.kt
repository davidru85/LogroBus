package com.ruizurraca.logrobus.android.feature.lines

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruizurraca.logrobus.android.core.designsystem.LineBadge
import com.ruizurraca.logrobus.android.core.designsystem.MoreActionButton
import com.ruizurraca.logrobus.android.core.designsystem.ScreenTitle
import com.ruizurraca.logrobus.android.core.designsystem.Tokens
import com.ruizurraca.logrobus.android.core.navigation.ScrollToTopOnReselect
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.feature.lines.presentation.LineGroupUi
import com.ruizurraca.logrobus.feature.lines.presentation.LineRowUi
import com.ruizurraca.logrobus.feature.lines.presentation.LinesEvent
import com.ruizurraca.logrobus.feature.lines.presentation.LinesUiState
import com.ruizurraca.logrobus.transit.domain.LineKind
import org.koin.androidx.compose.koinViewModel

/** The root of the Lines tab (LB-101), with its ViewModel from the graph (ADR-0014). [onOpenMore] opens More. */
@Composable
fun LinesRoute(
    onOpenMore: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LinesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LinesScreen(state, viewModel::onEvent, onOpenMore, modifier)
}

/** Renders [state] under the Lines top app bar, with the More action, and sends [onEvent]. */
@Composable
fun LinesScreen(
    state: LinesUiState,
    onEvent: (LinesEvent) -> Unit,
    onOpenMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { ScreenTitle(stringResource(R.string.lines_title)) },
                actions = { MoreActionButton(onClick = onOpenMore) },
            )
        },
    ) { padding ->
        val retry = { onEvent(LinesEvent.Retry) }
        val centred = Modifier.padding(padding).consumeWindowInsets(padding)
        when (state) {
            // Spec §7.3: the final layout with placeholders, never a lone spinner.
            LinesUiState.Loading -> PlaceholderRows(padding)
            is LinesUiState.Content -> LineList(state.groups, padding)
            LinesUiState.Empty -> Message(stringResource(R.string.lines_empty), retry, centred)
            is LinesUiState.Error -> Message(stringResource(state.reason.messageRes), retry, centred)
        }
    }
}

/** FR-LINE-01: each group under its heading. Re-selecting the tab at this root scrolls it to the top (FR-NAV-03). */
@Composable
private fun LineList(
    groups: List<LineGroupUi>,
    contentPadding: PaddingValues,
) {
    val listState = rememberLazyListState()
    ScrollToTopOnReselect(listState)
    // The padding goes inside the list, so the rows scroll behind the system bars rather than stop above them.
    LazyColumn(Modifier.consumeWindowInsets(contentPadding), listState, contentPadding) {
        groups.forEach { group ->
            item(key = group.kind) { GroupHeading(group.kind.titleRes) }
            items(group.lines, key = { it.id }) { LineRow(it) }
        }
    }
}

@Composable
private fun GroupHeading(
    @StringRes title: Int,
) {
    ListItem(
        headlineContent = {
            Text(
                stringResource(title),
                modifier = Modifier.semantics { heading() },
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall,
            )
        },
    )
}

/**
 * FR-LINE-02: the badge, the display name, which wraps rather than clips, and the status chip of a line that is not
 * operational. TalkBack reads the row as one: the line, its name, then its status (NFR-A11Y-02).
 */
@Composable
private fun LineRow(line: LineRowUi) {
    ListItem(
        headlineContent = { Text(line.displayName) },
        modifier = Modifier.semantics(mergeDescendants = true) {},
        leadingContent = { LineBadge(line.code, line.color, line.onColor) },
        supportingContent = if (line.isOperational) null else ({ StatusChip(stringResource(R.string.lines_not_operational)) }),
    )
}

/** A label, not an action: a line's status, in the error container's colours. */
@Composable
private fun StatusChip(text: String) {
    Surface(
        modifier = Modifier.padding(top = Tokens.Spacing.xs),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = Tokens.Spacing.s, vertical = Tokens.Spacing.xs),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/** Spec §7.3: rows shaped like the list's, hidden from accessibility services, while the first load runs. */
@Composable
private fun PlaceholderRows(contentPadding: PaddingValues) {
    val lineHeight = with(LocalDensity.current) { MaterialTheme.typography.bodyLarge.lineHeight.toDp() }
    Column(
        Modifier
            .consumeWindowInsets(contentPadding)
            .padding(contentPadding)
            .clearAndSetSemantics {},
    ) {
        repeat(PLACEHOLDER_ROWS) {
            ListItem(
                headlineContent = { Placeholder(Modifier.fillMaxWidth(PLACEHOLDER_WIDTH).height(lineHeight)) },
                leadingContent = { Placeholder(Modifier.size(Tokens.minTouchTarget, lineHeight)) },
            )
        }
    }
}

@Composable
private fun Placeholder(modifier: Modifier) {
    Box(modifier.clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant))
}

/** FR-ERR-05 and spec §7.3: one sentence, and Retry. */
@Composable
private fun Message(
    text: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().padding(Tokens.Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xl, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, textAlign = TextAlign.Center)
        Button(onClick = onRetry) { Text(stringResource(R.string.error_retry)) }
    }
}

@get:StringRes
private val LineKind.titleRes: Int
    get() =
        when (this) {
            LineKind.DAY -> R.string.lines_group_day
            LineKind.NIGHT -> R.string.lines_group_night
        }

@get:StringRes
private val ErrorReason.messageRes: Int
    get() =
        when (this) {
            ErrorReason.NETWORK -> R.string.error_network
            ErrorReason.SERVER -> R.string.error_server
            ErrorReason.DATA -> R.string.error_data
        }

// Enough rows to fill a phone's first screen.
private const val PLACEHOLDER_ROWS = 8

// A placeholder name spans this share of its row: long enough to read as text, short enough not to look like content.
private const val PLACEHOLDER_WIDTH = 0.6f
