package com.ruizurraca.logrobus.android.feature.lines

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruizurraca.logrobus.android.core.designsystem.FavouriteToggle
import com.ruizurraca.logrobus.android.core.designsystem.LineBadge
import com.ruizurraca.logrobus.android.core.designsystem.LineBadgeSize
import com.ruizurraca.logrobus.android.core.designsystem.MoreActionButton
import com.ruizurraca.logrobus.android.core.designsystem.RetryMessage
import com.ruizurraca.logrobus.android.core.designsystem.ScreenTitle
import com.ruizurraca.logrobus.android.core.designsystem.Tokens
import com.ruizurraca.logrobus.android.core.designsystem.messageRes
import com.ruizurraca.logrobus.android.core.navigation.ScrollToTopOnReselect
import com.ruizurraca.logrobus.feature.lines.presentation.FavouriteLineCardUi
import com.ruizurraca.logrobus.feature.lines.presentation.LineRowUi
import com.ruizurraca.logrobus.feature.lines.presentation.LinesEvent
import com.ruizurraca.logrobus.feature.lines.presentation.LinesUiState
import com.ruizurraca.logrobus.transit.domain.LineKind
import org.koin.androidx.compose.koinViewModel

/**
 * The root of the Lines tab (LB-101, LB-103), with its ViewModel from the graph (ADR-0014). [onOpenMore] opens More,
 * and [onOpenLine] a line's detail.
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
 * opens its line with [onOpenLine].
 */
@Composable
fun LinesScreen(
    state: LinesUiState,
    onEvent: (LinesEvent) -> Unit,
    onOpenMore: () -> Unit,
    onOpenLine: (lineId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
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
            LinesUiState.Loading -> PlaceholderRows(padding)

            is LinesUiState.Content -> LineList(state, padding, onEvent, onOpenLine)

            LinesUiState.Empty -> RetryMessage(stringResource(R.string.lines_empty), retry, centred)

            is LinesUiState.Error -> RetryMessage(stringResource(state.reason.messageRes), retry, centred)
        }
    }
}

/**
 * The favourite lines (FR-LINE-12), then each group under its heading (FR-LINE-01). Re-selecting the tab at this root
 * scrolls it to the top (FR-NAV-03).
 */
@Composable
private fun LineList(
    content: LinesUiState.Content,
    contentPadding: PaddingValues,
    onEvent: (LinesEvent) -> Unit,
    onOpenLine: (lineId: Int) -> Unit,
) {
    val listState = rememberLazyListState()
    ScrollToTopOnReselect(listState)
    // The padding goes inside the list, so the rows scroll behind the system bars rather than stop above them.
    LazyColumn(Modifier.consumeWindowInsets(contentPadding), listState, contentPadding) {
        item(key = FAVOURITES_HEADING) { GroupHeading(R.string.lines_favourites_title) }
        item(key = FAVOURITES) { FavouriteLines(content.favourites, onOpenLine) }
        content.groups.forEach { group ->
            item(key = group.kind) { GroupHeading(group.kind.titleRes) }
            items(group.lines, key = { it.id }) { LineRow(it, onEvent, onOpenLine) }
        }
    }
}

/**
 * FR-LINE-12: the favourite lines' cards in the user's order, in a carousel of fixed-width cards. With none, one hint
 * card, so the section never disappears (FR-LINE-14).
 */
@Composable
private fun FavouriteLines(
    cards: List<FavouriteLineCardUi>,
    onOpenLine: (lineId: Int) -> Unit,
) {
    if (cards.isEmpty()) {
        OutlinedCard(Modifier.padding(horizontal = Tokens.Spacing.l).fillMaxWidth()) {
            Text(
                stringResource(R.string.lines_favourites_hint),
                modifier = Modifier.padding(Tokens.Spacing.l),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }
    val carousel = rememberCarouselState { cards.size }
    // The carousel centres each card in its height, so every card takes the tallest one's: a copy of each, measured at
    // the card width and hidden from TalkBack, sets it. Names and headsigns then wrap at 200 % without a ragged row.
    SubcomposeLayout { constraints ->
        val cardWidth = CARD_WIDTH.roundToPx()
        val tallest =
            subcompose(MEASURED_CARDS) {
                cards.forEach { card -> Box(Modifier.clearAndSetSemantics {}) { FavouriteCard(card, onOpenLine = {}) } }
            }.maxOf { it.measure(Constraints(maxWidth = cardWidth)).height }
        val shown =
            subcompose(SHOWN_CARDS) {
                HorizontalUncontainedCarousel(
                    state = carousel,
                    itemWidth = CARD_WIDTH,
                    modifier = Modifier.height(tallest.toDp()),
                    itemSpacing = Tokens.Spacing.s,
                    contentPadding = PaddingValues(horizontal = Tokens.Spacing.l),
                ) { index ->
                    FavouriteCard(cards[index], onOpenLine, Modifier.fillMaxHeight().maskClip(CardDefaults.shape))
                }
            }.single().measure(constraints)
        layout(shown.width, shown.height) { shown.place(0, 0) }
    }
}

/**
 * FR-LINE-13: the badge, the display name in at most two lines, and both headsigns, "→ Hospital San Pedro · → Lardero".
 * TalkBack reads the card as one, the headsigns as "Towards …", and opens the line on a double tap.
 */
@Composable
private fun FavouriteCard(
    card: FavouriteLineCardUi,
    onOpenLine: (lineId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val towards = card.headsigns.map { stringResource(R.string.lines_towards, it) }.joinToString(", ")
    Card(onClick = { onOpenLine(card.id) }, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(Tokens.Spacing.m), verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.s)) {
            LineBadge(card.code, card.color, card.onColor)
            Text(
                card.displayName,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
            )
            if (card.headsigns.isNotEmpty()) {
                Text(
                    card.headsigns.joinToString(HEADSIGN_SEPARATOR) { "$HEADSIGN_ARROW $it" },
                    modifier = Modifier.semantics { contentDescription = towards },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
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
    ListItem(
        headlineContent = { Text(line.displayName) },
        modifier = Modifier.clickable { onOpenLine(line.id) },
        leadingContent = { LineBadge(line.code, line.color, line.onColor) },
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
 * Spec §7.3: a heading and rows shaped like the list's while the first load runs. TalkBack hears "Loading lines"
 * rather than the shapes (design_guidelines.md §8).
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
        ListItem(headlineContent = { Placeholder(Modifier.fillMaxWidth(HEADING_WIDTH).height(lineHeight)) })
        repeat(PLACEHOLDER_ROWS) {
            ListItem(
                headlineContent = { Placeholder(Modifier.fillMaxWidth(PLACEHOLDER_WIDTH).height(lineHeight)) },
                leadingContent = { Placeholder(Modifier.size(badge.minWidth, badge.minHeight)) },
            )
        }
    }
}

@Composable
private fun Placeholder(modifier: Modifier) {
    Box(modifier.clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant))
}

@get:StringRes
private val LineKind.titleRes: Int
    get() =
        when (this) {
            LineKind.DAY -> R.string.lines_group_day
            LineKind.NIGHT -> R.string.lines_group_night
        }

private const val FAVOURITES_HEADING = "favourites-heading"
private const val FAVOURITES = "favourites"
private const val MEASURED_CARDS = "measured-cards"
private const val SHOWN_CARDS = "shown-cards"

// Wide enough for a two-line name and a short headsign; a phone shows two cards and the edge of a third.
private val CARD_WIDTH = 168.dp

// Symbols, the same in every language; TalkBack reads the headsigns as words (lines_towards).
private const val HEADSIGN_ARROW = "→"
private const val HEADSIGN_SEPARATOR = " · "

// Enough rows to fill a phone's first screen.
private const val PLACEHOLDER_ROWS = 8

// A placeholder heading is shorter than a name, as "Líneas diurnas" is.
private const val HEADING_WIDTH = 0.35f

// A placeholder name spans this share of its row: long enough to read as text, short enough not to look like content.
private const val PLACEHOLDER_WIDTH = 0.6f
