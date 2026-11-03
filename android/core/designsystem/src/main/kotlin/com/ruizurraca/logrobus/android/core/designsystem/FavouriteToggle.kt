package com.ruizurraca.logrobus.android.core.designsystem

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource

/**
 * The favourite toggle of a line or stop (FR-LINE-15): a star, filled in the primary colour once it is a favourite, at
 * least 48 dp to touch. [label] says what a tap does, such as "Add line 3 to favourites", so it is a button: as a
 * checkbox, TalkBack would also say "checked" beside "Remove line 3 from favourites". [onToggle] gets the new value.
 */
@Composable
fun FavouriteToggle(
    isFavourite: Boolean,
    onToggle: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colour = if (isFavourite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    IconButton(
        onClick = { onToggle(!isFavourite) },
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(contentColor = colour),
    ) {
        Icon(
            painterResource(if (isFavourite) R.drawable.ic_favourite_selected else R.drawable.ic_favourite),
            contentDescription = label,
        )
    }
}
