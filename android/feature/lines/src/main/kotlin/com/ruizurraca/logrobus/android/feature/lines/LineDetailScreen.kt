package com.ruizurraca.logrobus.android.feature.lines

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruizurraca.logrobus.android.core.designsystem.FavouriteToggle
import com.ruizurraca.logrobus.android.core.designsystem.LineBadge
import com.ruizurraca.logrobus.android.core.designsystem.LineBadgeSize
import com.ruizurraca.logrobus.android.core.designsystem.RetryMessage
import com.ruizurraca.logrobus.android.core.designsystem.ScreenTitle
import com.ruizurraca.logrobus.android.core.designsystem.UpButton
import com.ruizurraca.logrobus.android.core.designsystem.messageRes
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailEvent
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailUi
import com.ruizurraca.logrobus.feature.lines.presentation.LineDetailUiState
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
    LineDetailScreen(state, viewModel::onEvent, onBack, modifier)
}

/**
 * A line's detail: the line and its favourite toggle (LB-103, FR-LINE-09), until LB-104 adds its map, direction toggle
 * and stops. The toolbar names the line and holds the toggle, labelled as the rows' are.
 */
@Composable
fun LineDetailScreen(
    state: LineDetailUiState,
    onEvent: (LineDetailEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val line = (state as? LineDetailUiState.Content)?.line
    Scaffold(
        modifier = modifier,
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
                LineHeader(state.line, padding)
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
private fun LineHeader(
    line: LineDetailUi,
    contentPadding: PaddingValues,
) {
    ListItem(
        headlineContent = { Text(line.displayName, style = MaterialTheme.typography.titleMedium) },
        modifier =
            Modifier
                .consumeWindowInsets(contentPadding)
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .semantics(mergeDescendants = true) {},
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
