package com.ruizurraca.logrobus.android.feature.lines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.ruizurraca.logrobus.android.core.designsystem.LineBadge
import com.ruizurraca.logrobus.android.core.designsystem.Tokens
import com.ruizurraca.logrobus.feature.lines.presentation.FavouriteLineCardUi
import com.ruizurraca.logrobus.feature.lines.presentation.LinesEvent
import com.ruizurraca.logrobus.feature.lines.presentation.LinesUiState

/**
 * FR-LINE-12: the favourite lines' cards in the user's order, in a carousel of fixed-width cards. With none, one hint
 * card, so the section never disappears (FR-LINE-14).
 */
@Composable
internal fun FavouriteLines(
    cards: List<FavouriteLineCardUi>,
    onOpenLine: (lineId: Int) -> Unit,
) {
    if (cards.isEmpty()) {
        val spoken = stringResource(R.string.lines_favourites_hint_spoken)
        OutlinedCard(Modifier.padding(horizontal = Tokens.Spacing.l).fillMaxWidth()) {
            Text(
                stringResource(R.string.lines_favourites_hint),
                // TalkBack reads words, never "white star".
                modifier = Modifier.padding(Tokens.Spacing.l).semantics { contentDescription = spoken },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }
    val carousel = rememberCarouselState { cards.size }
    val fontScale = LocalDensity.current.fontScale
    // The carousel centres each card in its height, so every card takes the tallest one's: a copy of each, measured at
    // the card width and hidden from TalkBack, sets it. Names and headsigns then wrap at 200 % without a ragged row.
    // Cards widen with the text, up to most of the row, so at 200 % a name keeps more than a word per line.
    SubcomposeLayout { constraints ->
        val cardWidth =
            minOf(CARD_WIDTH.toPx() * fontScale.coerceAtLeast(1f), constraints.maxWidth * MAX_CARD_SHARE).toInt()
        val tallest =
            subcompose(MEASURED_CARDS) {
                cards.forEach { card -> Box(Modifier.clearAndSetSemantics {}) { FavouriteCard(card, onOpen = null) } }
            }.maxOf { it.measure(Constraints(maxWidth = cardWidth)).height }
        val shown =
            subcompose(SHOWN_CARDS) {
                HorizontalUncontainedCarousel(
                    state = carousel,
                    itemWidth = cardWidth.toDp(),
                    modifier = Modifier.height(tallest.toDp()),
                    itemSpacing = Tokens.Spacing.s,
                    contentPadding = PaddingValues(horizontal = Tokens.Spacing.l),
                ) { index ->
                    val card = cards[index]
                    FavouriteCard(card, { onOpenLine(card.id) }, Modifier.fillMaxHeight().maskClip(CardDefaults.shape))
                }
            }.single().measure(constraints)
        layout(shown.width, shown.height) { shown.place(0, 0) }
    }
}

/**
 * FR-LINE-13: the badge, the display name in at most two lines, and both headsigns, "→ Hospital San Pedro · → Lardero".
 * TalkBack reads the card as one, the headsigns as "Towards …", and opens the line on a double tap ("open line"). With
 * no [onOpen], the copy that is only measured, it takes no input.
 */
@Composable
private fun FavouriteCard(
    card: FavouriteLineCardUi,
    onOpen: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val towards = card.headsigns.map { stringResource(R.string.lines_towards, it) }.joinToString(", ")
    val open = stringResource(R.string.lines_open_line)
    val clickable =
        onOpen?.let { action ->
            Modifier.clip(CardDefaults.shape).clickable(onClickLabel = open, role = Role.Button, onClick = action)
        } ?: Modifier
    Card(modifier = modifier.fillMaxWidth().then(clickable)) {
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
                    card.headsigns.joinToString(HEADSIGN_SEPARATOR) { "$HEADSIGN_ARROW$NO_BREAK_SPACE$it" },
                    modifier = Modifier.semantics { contentDescription = towards },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private const val MEASURED_CARDS = "measured-cards"
private const val SHOWN_CARDS = "shown-cards"

// Wide enough for a two-line name and a short headsign; a phone shows two cards and the edge of a third.
private val CARD_WIDTH = 168.dp

// At large text, cards widen with it, up to this share of the row, so the next card's edge still shows.
private const val MAX_CARD_SHARE = 0.8f

// Symbols, the same in every language; TalkBack reads the headsigns as words (lines_towards). No-break spaces keep
// each arrow with its headsign, and the dot with the headsign before it, when the text wraps.
private const val NO_BREAK_SPACE = '\u00A0'
private const val HEADSIGN_ARROW = "→"
private const val HEADSIGN_SEPARATOR = "$NO_BREAK_SPACE· "

/**
 * LB-110: the favourites' heading, with Edit while there is a favourite to edit, even one with no card. Without Edit
 * it keeps Edit's height, so the list never moves when Edit comes or goes.
 */
@Composable
internal fun FavouritesHeading(
    canEdit: Boolean,
    edit: FocusRequester,
    onEdit: () -> Unit,
) {
    GroupHeading(R.string.lines_favourites_title) {
        if (canEdit) {
            EditButton(Modifier.focusRequester(edit).focusable(), onEdit)
        } else {
            Spacer(Modifier.size(Tokens.minTouchTarget))
        }
    }
}

/** LB-110: the edit sheet while [editing]. Closing it gives the focus back to [edit], which opened it (§8). */
@Composable
internal fun FavouritesEditSheet(
    state: LinesUiState,
    editing: Boolean,
    edit: FocusRequester,
    onEvent: (LinesEvent) -> Unit,
    onClose: () -> Unit,
) {
    var closed by remember { mutableStateOf(false) }
    LaunchedEffect(closed) {
        if (closed && state is LinesUiState.Content && state.editable.isNotEmpty()) edit.requestFocus()
        closed = false
    }
    if (editing && state is LinesUiState.Content) {
        FavouriteLinesSheet(
            state.editable,
            onEvent,
            onDismiss = {
                onClose()
                closed = true
            },
        )
    }
}

/** LB-110: opens the edit sheet. TalkBack names it by what it edits, a name that keeps the word it shows. */
@Composable
private fun EditButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val name = stringResource(R.string.lines_favourites_edit_title)
    TextButton(onClick = onClick, modifier = modifier.semantics { contentDescription = name }) {
        Text(stringResource(R.string.lines_favourites_edit))
    }
}
