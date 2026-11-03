package com.ruizurraca.logrobus.android.feature.lines

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruizurraca.logrobus.android.core.designsystem.FavouriteToggle
import com.ruizurraca.logrobus.android.core.designsystem.LineBadge
import com.ruizurraca.logrobus.android.core.designsystem.LineBadgeSize
import com.ruizurraca.logrobus.android.core.designsystem.MoreActionButton
import com.ruizurraca.logrobus.android.core.designsystem.RetryMessage
import com.ruizurraca.logrobus.android.core.designsystem.ScreenTitle
import com.ruizurraca.logrobus.android.core.designsystem.SearchField
import com.ruizurraca.logrobus.android.core.designsystem.SearchFieldPlaceholder
import com.ruizurraca.logrobus.android.core.designsystem.Tokens
import com.ruizurraca.logrobus.android.core.designsystem.messageRes
import com.ruizurraca.logrobus.android.core.navigation.ScrollToTopOnReselect
import com.ruizurraca.logrobus.feature.lines.presentation.LineGroupUi
import com.ruizurraca.logrobus.feature.lines.presentation.LineRowUi
import com.ruizurraca.logrobus.feature.lines.presentation.LinesEvent
import com.ruizurraca.logrobus.feature.lines.presentation.LinesUiState
import com.ruizurraca.logrobus.transit.domain.LineKind
import kotlinx.coroutines.flow.dropWhile
import org.koin.androidx.compose.koinViewModel
import com.ruizurraca.logrobus.android.core.designsystem.R as DS

/**
 * The root of the Lines tab (LB-101, LB-103, LB-102), with its ViewModel from the graph (ADR-0014). [onOpenMore] opens
 * More, and [onOpenLine] a line's detail.
 */
@Composable
fun LinesRoute(
    onOpenMore: () -> Unit,
    onOpenLine: (lineId: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LinesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LinesScreen(state, viewModel::onEvent, onOpenMore, onOpenLine, modifier)
}

/**
 * Renders [state] under the Lines top app bar, with the More action, and sends [onEvent]. A favourite card or a row
 * opens its line with [onOpenLine]. [search] holds the search field's text, which the screen sends as it changes
 * (FR-LINE-03); it survives configuration changes and process death.
 */
@Suppress("LongParameterList") // State and events, two ways out to the shell, the modifier, the field's hoisted state.
@Composable
fun LinesScreen(
    state: LinesUiState,
    onEvent: (LinesEvent) -> Unit,
    onOpenMore: () -> Unit,
    onOpenLine: (lineId: Int) -> Unit,
    modifier: Modifier = Modifier,
    search: TextFieldState = rememberTextFieldState(),
) {
    val send by rememberUpdatedState(onEvent)
    // The ViewModel starts with no query, so an empty field is news only once it has had text. A field restored with
    // text, after a configuration change or process death, filters the list again. This relies on the field's saved
    // state and the ViewModel sharing one lifetime, the navigation entry's (AppShell), so neither outlives the other.
    LaunchedEffect(search) {
        snapshotFlow { search.text.toString() }.dropWhile(String::isEmpty).collect { send(LinesEvent.Search(it)) }
    }
    // The bar takes its scrolled colour while the list runs under it. It does not collapse yet: a scroll to the top on
    // re-selection (FR-NAV-03) is programmatic, so it would not bring a collapsed bar back.
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { ScreenTitle(stringResource(R.string.lines_title)) },
                actions = { MoreActionButton(onClick = onOpenMore) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        val retry = { onEvent(LinesEvent.Retry) }
        val centred = Modifier.padding(padding).consumeWindowInsets(padding)
        when (state) {
            // Spec §7.3: the final layout with placeholders, never a lone spinner.
            LinesUiState.Loading -> {
                PlaceholderRows(padding)
            }

            // One branch, so the list and its search field stay the same as the results change: the field keeps its
            // text, its focus and the keyboard.
            is LinesUiState.Content, is LinesUiState.SearchResults, LinesUiState.NoMatches -> {
                LineList(state, search, padding, onEvent, onOpenLine)
            }

            LinesUiState.Empty -> {
                RetryMessage(stringResource(R.string.lines_empty), retry, centred)
            }

            is LinesUiState.Error -> {
                RetryMessage(stringResource(state.reason.messageRes), retry, centred)
            }
        }
    }
}

/**
 * The search field (FR-LINE-03), then the favourite lines (FR-LINE-12) and each group under its heading (FR-LINE-01).
 * While a search is active, its results in the same groups, without the favourites (FR-LINE-16), or a sentence and
 * "Clear search". Re-selecting the tab at this root scrolls it to the top (FR-NAV-03).
 */
@Composable
private fun LineList(
    state: LinesUiState,
    search: TextFieldState,
    contentPadding: PaddingValues,
    onEvent: (LinesEvent) -> Unit,
    onOpenLine: (lineId: Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val field = remember { FocusRequester() }
    ScrollToTopOnReselect(listState)
    // The padding goes inside the list, so the rows scroll behind the system bars rather than stop above them. The list
    // ends above the keyboard: its first item is the field, so the window never pans to show the last results.
    LazyColumn(Modifier.consumeWindowInsets(contentPadding).imePadding(), listState, contentPadding) {
        item(key = SEARCH) {
            SearchField(
                search,
                stringResource(R.string.lines_search),
                SearchFieldPadding.fillMaxWidth().focusRequester(field),
            )
        }
        when (state) {
            is LinesUiState.Content -> {
                item(key = FAVOURITES_HEADING) { GroupHeading(R.string.lines_favourites_title) }
                item(key = FAVOURITES) { FavouriteLines(state.favourites, onOpenLine) }
                groups(state.groups, onEvent, onOpenLine)
            }

            is LinesUiState.SearchResults -> {
                groups(state.groups, onEvent, onOpenLine)
            }

            LinesUiState.NoMatches -> {
                item(key = NO_MATCHES) {
                    // The button goes with this state, so the focus moves to the field rather than get lost.
                    NoMatches(
                        onClear = {
                            search.clearText()
                            field.requestFocus()
                        },
                    )
                }
            }

            // Not a list: LinesScreen shows them on their own.
            LinesUiState.Loading, LinesUiState.Empty, is LinesUiState.Error -> {
                Unit
            }
        }
    }
}

private fun LazyListScope.groups(
    groups: List<LineGroupUi>,
    onEvent: (LinesEvent) -> Unit,
    onOpenLine: (lineId: Int) -> Unit,
) {
    groups.forEach { group ->
        item(key = group.kind) { GroupHeading(group.kind.titleRes) }
        items(group.lines, key = { it.id }) { LineRow(it, onEvent, onOpenLine) }
    }
}

/**
 * Spec §7.3: a search that found no line says so in one sentence and offers one action, which empties the field.
 * TalkBack announces the sentence as the pane's title when it appears, without moving its focus (NFR-A11Y): a live
 * region would not speak, since TalkBack only hears one whose content changes, and this node is new.
 */
@Composable
private fun NoMatches(onClear: () -> Unit) {
    val message = stringResource(R.string.lines_no_matches)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(Tokens.Spacing.xl)
            .semantics { paneTitle = message },
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, textAlign = TextAlign.Center)
        Button(onClick = onClear) { Text(stringResource(DS.string.search_clear)) }
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
 * operational. TalkBack reads the row as one: the line, its name, then its status (NFR-A11Y-02); a double tap opens
 * the line. The favourite toggle is its own element, labelled by the line (FR-LINE-15).
 */
@Composable
private fun LineRow(
    line: LineRowUi,
    onEvent: (LinesEvent) -> Unit,
    onOpenLine: (lineId: Int) -> Unit,
) {
    val toggle = if (line.isFavourite) R.string.lines_favourite_remove else R.string.lines_favourite_add
    val badge: @Composable () -> Unit = { LineBadge(line.code, line.color, line.onColor) }
    // At large text the badge goes above the name, which then has the row's width beside the toggle and breaks between
    // words rather than inside one ("Valdegastea").
    val stacked = LocalDensity.current.fontScale >= LARGE_TEXT_SCALE
    ListItem(
        headlineContent = { Text(line.displayName) },
        modifier =
            Modifier.clickable(onClickLabel = stringResource(R.string.lines_open_line), role = Role.Button) {
                onOpenLine(line.id)
            },
        overlineContent = if (stacked) badge else null,
        leadingContent = if (stacked) null else badge,
        supportingContent = if (line.isOperational) null else notOperational,
        trailingContent = {
            FavouriteToggle(
                isFavourite = line.isFavourite,
                onToggle = { onEvent(LinesEvent.SetFavourite(line.id, it)) },
                label = stringResource(toggle, line.code),
            )
        },
    )
}

private val notOperational: @Composable () -> Unit = { StatusChip(stringResource(R.string.lines_not_operational)) }

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

/**
 * Spec §7.3: the favourites heading and hint, then a heading and rows shaped like the list's, while the first load
 * runs, so nothing moves when the lines arrive. TalkBack hears "Loading lines" rather than the shapes
 * (design_guidelines.md §8).
 */
@Composable
private fun PlaceholderRows(contentPadding: PaddingValues) {
    val lineHeight =
        with(LocalDensity.current) {
            MaterialTheme.typography.bodyLarge.lineHeight
                .toDp()
        }
    val loading = stringResource(R.string.lines_loading)
    val badge = LineBadgeSize.Regular
    Column(
        Modifier
            .consumeWindowInsets(contentPadding)
            .padding(contentPadding)
            .clearAndSetSemantics { contentDescription = loading },
    ) {
        SearchFieldPlaceholder(SearchFieldPadding.fillMaxWidth())
        ListItem(headlineContent = { Placeholder(Modifier.fillMaxWidth(HEADING_WIDTH).height(lineHeight)) })
        Placeholder(
            Modifier
                .padding(horizontal = Tokens.Spacing.l)
                .fillMaxWidth()
                .height(lineHeight + Tokens.Spacing.l * 2),
        )
        ListItem(headlineContent = { Placeholder(Modifier.fillMaxWidth(HEADING_WIDTH).height(lineHeight)) })
        repeat(PLACEHOLDER_ROWS) {
            ListItem(
                headlineContent = { Placeholder(Modifier.fillMaxWidth(PLACEHOLDER_WIDTH).height(lineHeight)) },
                leadingContent = { Placeholder(Modifier.size(badge.minWidth, badge.minHeight)) },
                trailingContent = { Placeholder(Modifier.size(Tokens.Spacing.xl)) },
            )
        }
    }
}

/** A shape that stands for content while it loads, in the surface variant colour. */
@Composable
internal fun Placeholder(modifier: Modifier) {
    Box(modifier.clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant))
}

@get:StringRes
private val LineKind.titleRes: Int
    get() =
        when (this) {
            LineKind.DAY -> R.string.lines_group_day
            LineKind.NIGHT -> R.string.lines_group_night
        }

private const val SEARCH = "search"
private const val NO_MATCHES = "no-matches"
private const val FAVOURITES_HEADING = "favourites-heading"
private const val FAVOURITES = "favourites"

// The search field lines up with the rows' content, and keeps its distance from the first heading.
private val SearchFieldPadding = Modifier.padding(horizontal = Tokens.Spacing.l, vertical = Tokens.Spacing.s)

// From this font scale, a row puts its badge above its name (Android's 200 % is 2).
private const val LARGE_TEXT_SCALE = 1.5f

// Enough rows to fill a phone's first screen.
private const val PLACEHOLDER_ROWS = 8

// A placeholder heading is shorter than a name, as "Líneas diurnas" is.
private const val HEADING_WIDTH = 0.35f

// A placeholder name spans this share of its row: long enough to read as text, short enough not to look like content.
internal const val PLACEHOLDER_WIDTH = 0.6f
