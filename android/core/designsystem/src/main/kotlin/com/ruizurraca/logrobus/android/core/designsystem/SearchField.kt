package com.ruizurraca.logrobus.android.core.designsystem

import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction

/**
 * A search field that filters what is under it as the user types (LB-102), in the search bar's shape and colours, with
 * a clear button once it has text. M3's collapsed `SearchBar` only opens an expanded search view, and keeps the keyboard
 * away until it does. [placeholder] says what it searches; the keyboard's search key closes the keyboard.
 */
@Composable
fun SearchField(
    state: TextFieldState,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val container = SearchBarDefaults.colors().containerColor
    TextField(
        state = state,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon =
            if (state.text.isEmpty()) {
                null
            } else {
                {
                    IconButton(onClick = { state.clearText() }) {
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
