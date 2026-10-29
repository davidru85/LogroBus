package com.ruizurraca.logrobus.android.core.designsystem

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

/** The toolbar action of every tab root that opens More (FR-NAV-04), with a settings-style icon (design_guidelines.md §2.1). */
@Composable
fun MoreActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(painterResource(R.drawable.ic_more), contentDescription = stringResource(R.string.action_more))
    }
}

/** The navigation icon of a pushed screen's top app bar, back to the previous screen of the tab. */
@Composable
fun UpButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painterResource(R.drawable.ic_navigate_up),
            contentDescription = stringResource(R.string.action_navigate_up),
        )
    }
}
