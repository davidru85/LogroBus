package com.ruizurraca.logrobus.android.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max

/**
 * A search field that filters what is under it as the user types (LB-102), in the search bar's shape and colours,
 * with a clear button once it has text. M3's collapsed `SearchBar` only opens an expanded search view, and keeps the
 * keyboard away until it does. [placeholder] says what it searches, and stays its name for TalkBack once it has text,
 * as M3's input field does. Clearing it leaves the focus in it; the keyboard's search key closes the keyboard.
 */
@Composable
fun SearchField(
    state: TextFieldState,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    val container = SearchBarDefaults.colors().containerColor
    TextField(
        state = state,
        modifier = modifier.focusRequester(focus).semantics { contentDescription = placeholder },
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon =
            if (state.text.isEmpty()) {
                null
            } else {
                {
                    // The button goes with the text, so the focus moves to the field rather than get lost.
                    IconButton(
                        onClick = {
                            state.clearText()
                            focus.requestFocus()
                        },
                    ) {
                        Icon(painterResource(R.drawable.ic_clear), stringResource(R.string.search_clear))
                    }
                }
            },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        onKeyboardAction = { keyboard?.hide() },
        lineLimits = TextFieldLineLimits.SingleLine,
        shape = SearchBarDefaults.inputFieldShape,
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = container,
                unfocusedContainerColor = container,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
    )
}

/**
 * The search field's shape while a screen's first load runs (spec §7.3), in the placeholders' colour: as tall as the
 * field at every text size, so nothing moves when the content arrives.
 */
@Composable
fun SearchFieldPlaceholder(modifier: Modifier = Modifier) {
    // A single-line field is its text's line between its vertical padding, and never shorter than its minimum height.
    // Text sets a line at the line height's share of the scaled font size, which grows less than linearly at large
    // text.
    val style = MaterialTheme.typography.bodyLarge
    val line = with(LocalDensity.current) { style.fontSize.toDp() * (style.lineHeight.value / style.fontSize.value) }
    Box(
        modifier
            .height(max(TextFieldDefaults.MinHeight, line + FIELD_PADDING * 2))
            .clip(SearchBarDefaults.inputFieldShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

// TextField's vertical padding without a label.
private val FIELD_PADDING = 16.dp
